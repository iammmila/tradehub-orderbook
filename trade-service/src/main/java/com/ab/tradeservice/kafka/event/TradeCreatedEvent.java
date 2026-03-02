package com.ab.tradeservice.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeCreatedEvent(
        String eventId,
        int eventVersion,
        String instrument,
        BigDecimal price,
        Long quantity,
        Long buyOrderId,
        Long sellOrderId,
        Long buyerUserId,
        Long sellerUserId,
        String exchangeCode,
        LocalDateTime createdAt
) {}