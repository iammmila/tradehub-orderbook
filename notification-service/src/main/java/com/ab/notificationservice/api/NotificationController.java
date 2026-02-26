package com.ab.notificationservice.api;

import com.ab.notificationservice.dto.NotificationDto;
import com.ab.notificationservice.mapper.NotificationMapper;
import com.ab.notificationservice.security.AuthPrincipal;
import com.ab.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public ResponseEntity<Page<NotificationDto>> list(
            @AuthenticationPrincipal AuthPrincipal me,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var p = service.listForUser(me.userId(), page, size).map(NotificationMapper::toDto);
        return ResponseEntity.ok(p);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount(@AuthenticationPrincipal AuthPrincipal me) {
        return ResponseEntity.ok(service.unreadCount(me.userId()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long id
    ) {
        service.markRead(me.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal AuthPrincipal me) {
        service.markAllRead(me.userId());
        return ResponseEntity.noContent().build();
    }
}