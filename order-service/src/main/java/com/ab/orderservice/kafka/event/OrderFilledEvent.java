package com.ab.orderservice.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderFilledEvent(
        String eventId,
        Instant occurredAt,
        Long orderId,
        Long userId,
        String instrument,
        String side,
        BigDecimal price,
        Long filledQuantityTotal,   // total filled (usually original quantity)
        Long remainingQuantity,     // should be 0
        String status               // FILLED
) {}