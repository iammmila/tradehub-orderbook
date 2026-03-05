package com.ab.notificationservice.mapper;

import com.ab.notificationservice.dto.NotificationDto;
import com.ab.notificationservice.model.Notification;

/**
 * Usage:
 * - Pure mapping utility (no Spring bean) converting Notification entity to NotificationDto.
 * - Keeps controller/service layers free from manual field copying.
 */
public final class NotificationMapper {
    private NotificationMapper() {
    }

    /**
     * Maps entity -> DTO and derives "read" from readAt timestamp.
     * Keeps API contract stable even if the entity structure changes internally.
     */
    public static NotificationDto toDto(Notification e) {
        return NotificationDto.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .type(e.getType())
                .title(e.getTitle())
                .message(e.getMessage())
                .entityType(e.getEntityType())
                .entityId(e.getEntityId())
                .createdAt(e.getCreatedAt())
                .read(e.getReadAt() != null)
                .build();
    }
}
