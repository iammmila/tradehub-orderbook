package com.ab.orderservice.service.matching;

import com.ab.orderservice.kafka.OrderEventFactory;
import com.ab.orderservice.kafka.OrderEventsProducer;
import com.ab.orderservice.kafka.TradeEventFactory;
import com.ab.orderservice.kafka.TradeEventsProducer;
import com.ab.orderservice.kafka.event.TradeCreatedEvent;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class MatchingEventPublisher {

    private final TradeEventFactory tradeEventFactory;
    private final TradeEventsProducer tradeEventsProducer;
    private final OrderEventFactory orderEventFactory;
    private final OrderEventsProducer orderEventsProducer;

    /**
     * Publishes a trade-created event.
     */
    public void publishTradeCreated(Order buy, Order sell, BigDecimal price, long qty, LocalDateTime now) {
        TradeCreatedEvent event = tradeEventFactory.created(buy, sell, price, qty, now);
        tradeEventsProducer.publish(String.valueOf(buy.getId()), event);
    }

    /**
     * Publishes order fill events based on current status.
     */
    public void publishOrderFillIfNeeded(Order order, long filledNow) {
        if (order.getStatus() == OrderStatus.PARTIALLY_FILLED) {
            var ev = orderEventFactory.partiallyFilled(order, filledNow);
            orderEventsProducer.publish(String.valueOf(order.getId()), ev);
        } else if (order.getStatus() == OrderStatus.FILLED) {
            var ev = orderEventFactory.filled(order);
            orderEventsProducer.publish(String.valueOf(order.getId()), ev);
        }
    }
}