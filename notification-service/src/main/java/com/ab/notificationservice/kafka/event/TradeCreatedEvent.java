package com.ab.notificationservice.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeCreatedEvent(
        String eventId,
        Integer eventVersion,
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
