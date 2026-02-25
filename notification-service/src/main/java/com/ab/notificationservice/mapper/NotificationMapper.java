package com.ab.notificationservice.mapper;

import com.ab.notificationservice.dto.NotificationDto;
import com.ab.notificationservice.model.Notification;

public final class NotificationMapper {
    private NotificationMapper() {
    }

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
