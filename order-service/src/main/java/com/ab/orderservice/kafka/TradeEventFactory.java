package com.ab.orderservice.kafka;

import com.ab.orderservice.kafka.event.TradeCreatedEvent;
import com.ab.orderservice.model.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class TradeEventFactory {

    public TradeCreatedEvent created(Order buy, Order sell, BigDecimal price, long qty, LocalDateTime now) {
        return new TradeCreatedEvent(
                UUID.randomUUID().toString(),
                1,
                buy.getInstrument(),
                price,
                qty,
                buy.getId(),
                sell.getId(),
                buy.getUserId(),
                sell.getUserId(),
                buy.getExchangeCode(),
                now
        );
    }
}