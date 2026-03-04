package com.ab.tradeservice.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Kafka event fired when a trade is created in the system.
 * Usage:
 * - Produced by another service (e.g., matching engine / orderbook) and consumed by Trade Service.
 * - Designed as an immutable payload (record) to keep event handling predictable and thread-safe.
 */
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