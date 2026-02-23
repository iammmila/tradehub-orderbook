package com.ab.orderservice.kafka;

import com.ab.orderservice.kafka.event.*;
import com.ab.orderservice.model.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class OrderEventFactory {

    public OrderCreatedEvent created(Order o) {
        return new OrderCreatedEvent(
                newId(), Instant.now(),
                o.getId(), o.getUserId(), o.getInstrument(),
                o.getSide().name(),
                o.getPrice(), o.getQuantity(), o.getRemainingQuantity(),
                o.getStatus().name()
        );
    }

    public OrderCancelledEvent cancelled(Order o, String reason) {
        return new OrderCancelledEvent(
                newId(), Instant.now(),
                o.getId(), o.getUserId(), o.getInstrument(),
                o.getSide().name(),
                o.getStatus().name(),
                reason
        );
    }

    public OrderReplacedEvent replaced(Order before, Order after) {
        return new OrderReplacedEvent(
                newId(), Instant.now(),
                after.getId(), after.getUserId(), after.getInstrument(),
                after.getSide().name(),
                before.getPrice(), after.getPrice(),
                before.getQuantity(), after.getQuantity(),
                after.getRemainingQuantity(),
                after.getStatus().name()
        );
    }

    private String newId() {
        return UUID.randomUUID().toString();
    }
}