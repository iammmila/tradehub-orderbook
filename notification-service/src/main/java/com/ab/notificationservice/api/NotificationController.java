package com.ab.notificationservice.api;

import com.ab.notificationservice.dto.NotificationDto;
import com.ab.notificationservice.mapper.NotificationMapper;
import com.ab.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public ResponseEntity<Page<NotificationDto>> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var p = service.listForUser(userId, page, size).map(NotificationMapper::toDto);
        return ResponseEntity.ok(p);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.unreadCount(userId));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id
    ) {
        service.markRead((userId), id);
        return ResponseEntity.noContent().build(); // 204
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@RequestHeader("X-User-Id") Long userId) {
        service.markAllRead(userId);
        return ResponseEntity.noContent().build(); // 204
    }
}
