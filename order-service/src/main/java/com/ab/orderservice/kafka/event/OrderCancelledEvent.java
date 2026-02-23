package com.ab.orderservice.kafka.event;

import java.time.Instant;

public record OrderCancelledEvent(
        String eventId,
        Instant occurredAt,
        Long orderId,
        Long userId,
        String instrument,
        String side,
        String status,
        String reason
) { }