package com.ab.tradeservice.kafka;

import com.ab.tradeservice.dto.CreateTradeRequest;
import com.ab.tradeservice.kafka.event.TradeCreatedEvent;
import com.ab.tradeservice.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

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
        log.info("KAFKA RECEIVED eventId={} instrument={} buyOrderId={} sellOrderId={} qty={} price={}",
                event.eventId(),
                event.instrument(),
                event.buyOrderId(),
                event.sellOrderId(),
                event.quantity(),
                event.price());

        tradeService.createTrade(CreateTradeRequest.builder()
                .instrument(event.instrument())
                .price(event.price())
                .quantity(event.quantity())
                .buyOrderId(event.buyOrderId())
                .sellOrderId(event.sellOrderId())
                .buyerUserId(event.buyerUserId())
                .sellerUserId(event.sellerUserId())
                .createdAt(event.createdAt())
                .build());
    }
}