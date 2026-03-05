package com.ab.notificationservice.service;

import com.ab.notificationservice.exception.ApiException;
import com.ab.notificationservice.exception.enums.ErrorCode;
import com.ab.notificationservice.model.Notification;
import com.ab.notificationservice.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Usage:
 * - Application service for notification persistence and read-state updates.
 * - Enforces user scoping rules (a user can only mark their own notifications as read).
 * - Keeps validation and transactional boundaries centralized.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    /**
     * Creates a notification with basic validation and default timestamps.
     * Intended to be called from Kafka consumers and internal flows (not directly from controllers).
     */
    @Transactional
    public Notification create(Notification entity) {
        if (entity.getUserId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED);
        }
        if (isBlank(entity.getType()) || isBlank(entity.getTitle()) || isBlank(entity.getMessage())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED);
        }

        if (entity.getCreatedAt() == null) entity.setCreatedAt(Instant.now());

        return notificationRepository.save(entity);
    }

    /**
     * Returns newest-first notifications for the given user.
     * Read-only transaction improves performance and avoids accidental writes.
     */
    @Transactional(readOnly = true)
    public Page<Notification> listForUser(Long userId, int page, int size) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(
                userId,
                PageRequest.of(page, size)
        );
    }

    /**
     * Efficient badge counter endpoint support.
     * Uses a count query rather than fetching rows.
     */
    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    /**
     * Marks a single notification as read with ownership enforcement.
     * repeated calls do not change state once readAt is set.
     */
    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOTIFICATION_NOT_FOUND));
        // Prevent reading/updating another user's notifications
        if (!n.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED);
        }

        if (n.getReadAt() == null) {
            n.setReadAt(Instant.now());
            notificationRepository.save(n);
        }
    }

    /**
     * Bulk mark-as-read for the current user's latest notifications.
     * Uses a bounded page to avoid loading an unbounded dataset into memory.
     */
    @Transactional
    public int markAllRead(Long userId) {
        var page = notificationRepository.
                findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 500));
        var now = Instant.now();
        int changed = 0;

        for (var n : page.getContent()) {
            if (n.getReadAt() == null) {
                n.setReadAt(now);
                changed++;
            }
        }
        notificationRepository.saveAll(page.getContent());
        return changed;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}