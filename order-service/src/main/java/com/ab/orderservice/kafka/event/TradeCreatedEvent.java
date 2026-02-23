package com.ab.orderservice.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeCreatedEvent(
        String eventId,     // UUID
        int eventVersion,  // 1

        String instrument,
        BigDecimal price,
        Long quantity,

        Long buyOrderId,
        Long sellOrderId,

        Long buyerUserId,
        Long sellerUserId,

        LocalDateTime createdAt
) {
}