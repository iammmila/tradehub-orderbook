package com.ab.notificationservice.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreatedEvent(
        String eventId,
        Instant occurredAt,
        Long orderId,
        Long userId,
        String instrument,
        String side,
        BigDecimal price,
        Long quantity,
        Long remainingQuantity,
        String status
) {}