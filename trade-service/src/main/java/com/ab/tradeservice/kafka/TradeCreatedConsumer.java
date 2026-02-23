package com.ab.tradeservice.kafka;

import com.ab.tradeservice.dto.CreateTradeRequest;
import com.ab.tradeservice.kafka.event.TradeCreatedEvent;
import com.ab.tradeservice.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TradeCreatedConsumer {

    private final TradeService tradeService;

    @KafkaListener(
            topics = "${app.kafka.topics.trade-created}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTradeCreated(TradeCreatedEvent event) {

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