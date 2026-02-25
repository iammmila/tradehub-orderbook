package com.ab.orderservice.kafka.event;


import java.math.BigDecimal;
import java.time.Instant;

public record OrderPartiallyFilledEvent(
        String eventId,
        Instant occurredAt,
        Long orderId,
        Long userId,
        String instrument,
        String side,
        BigDecimal price,
        Long filledQuantity,        // filled in THIS match
        Long remainingQuantity,     // after this match
        String status               // PARTIALLY_FILLED
) {}
