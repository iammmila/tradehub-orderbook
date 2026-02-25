package com.ab.notificationservice.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderReplacedEvent(
        String eventId,
        Instant occurredAt,
        Long orderId,
        Long userId,
        String instrument,
        String side,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        Long oldQuantity,
        Long newQuantity,
        Long remainingQuantity,
        String status
) {}