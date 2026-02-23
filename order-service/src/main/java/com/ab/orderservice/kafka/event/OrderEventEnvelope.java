package com.ab.orderservice.kafka.event;

import java.time.Instant;

public record OrderEventEnvelope(
        String eventType,   // ORDER_CREATED / ORDER_CANCELLED / ORDER_REPLACED
        String eventId,
        Instant occurredAt,
        Long orderId,
        Object payload
) { }