package com.ab.tradeservice.kafka;

import com.ab.tradeservice.dto.CreateTradeRequest;
import com.ab.tradeservice.kafka.event.TradeCreatedEvent;
import com.ab.tradeservice.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for TradeCreatedEvent.
 * Usage:
 * - Listens to "trade-created" topic and converts events into a TradeService command (CreateTradeRequest).
 * - Keeps Kafka concerns (topics/deserialization/logging) separate from business logic (TradeService).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeCreatedConsumer {

    private final TradeService tradeService;

    @KafkaListener(
            topics = "${app.kafka.topics.trade-created}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTradeCreated(TradeCreatedEvent event) {
        // structured logging makes production support easier (search by eventId/orderId).
        log.info("KAFKA RECEIVED eventId={} instrument={} buyOrderId={} sellOrderId={} qty={} exchangeCode={} price={}",
                event.eventId(),
                event.instrument(),
                event.buyOrderId(),
                event.sellOrderId(),
                event.quantity(),
                event.exchangeCode(),
                event.price());

        //map event -> command DTO so the service layer stays transport-agnostic (Kafka/HTTP/etc.).
        tradeService.createTrade(CreateTradeRequest.builder()
                .instrument(event.instrument())
                .price(event.price())
                .quantity(event.quantity())
                .buyOrderId(event.buyOrderId())
                .sellOrderId(event.sellOrderId())
                .buyerUserId(event.buyerUserId())
                .sellerUserId(event.sellerUserId())
                .exchangeCode(event.exchangeCode())
                .createdAt(event.createdAt())
                .build());
    }
}