package com.ab.orderservice.service;

import com.ab.orderservice.kafka.OrderEventFactory;
import com.ab.orderservice.kafka.OrderEventsProducer;
import com.ab.orderservice.kafka.TradeEventFactory;
import com.ab.orderservice.kafka.TradeEventsProducer;
import com.ab.orderservice.kafka.event.TradeCreatedEvent;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchingService {
    private final OrderRepository orderRepository;
    private final TradeEventFactory tradeEventFactory;
    private final TradeEventsProducer tradeEventsProducer;
    private final OrderEventFactory orderEventFactory;
    private final OrderEventsProducer orderEventsProducer;

    private static final List<OrderStatus> ACTIVE_STATUSES =
            List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED);

    @Transactional
    public void match(Order incoming) {
        if (incoming.getRemainingQuantity() == null || incoming.getRemainingQuantity() <= 0) {
            return;
        }

        if (!ACTIVE_STATUSES.contains(incoming.getStatus())) {
            return;
        }

        // Normalize instrument
        String instrument = incoming.getInstrument().trim().toUpperCase();
        incoming.setInstrument(instrument);

        if (incoming.getExchangeCode() == null || incoming.getExchangeCode().isBlank()) {
            throw new IllegalStateException("Incoming order has null exchangeCode");
        }
        incoming.setExchangeCode(incoming.getExchangeCode().trim().toUpperCase());

        if (incoming.getSide() == OrderSide.BUY) {
            matchBuy(incoming);
        } else {
            matchSell(incoming);
        }

        orderRepository.save(incoming);
    }

    private void matchBuy(Order buy) {
        String ex = buy.getExchangeCode();
        // Get SELL candidates sorted by best price then FIFO
        List<Order> sells = orderRepository
                .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                        ex,
                        buy.getInstrument(),
                        OrderSide.SELL,
                        ACTIVE_STATUSES,
                        0L
                );
        Long buyerId = buy.getUserId();

        for (Order sell : sells) {
            if (buy.getRemainingQuantity() <= 0) break;

            //  Prevent self-trade
            if (sell.getUserId().equals(buyerId)) {
                continue;
            }

            boolean buyIsMarket = buy.getType() != null && buy.getType().name().equals("MARKET");
            if (!buyIsMarket) {
                if (buy.getPrice().compareTo(sell.getPrice()) < 0) {
                    break;
                }
            }

            long tradeQty = Math.min(buy.getRemainingQuantity(), sell.getRemainingQuantity());

            // Trade price = resting order price (sell price)
            BigDecimal tradePrice = sell.getPrice();
            LocalDateTime now = LocalDateTime.now();

            // Publish trade event to Kafka
            TradeCreatedEvent event = tradeEventFactory.created(buy, sell, tradePrice, tradeQty, now);
            tradeEventsProducer.publish(String.valueOf(buy.getId()), event);

            // Update quantities
            buy.setRemainingQuantity(buy.getRemainingQuantity() - tradeQty);
            sell.setRemainingQuantity(sell.getRemainingQuantity() - tradeQty);

            // Update statuses
            updateStatusAfterFill(buy);
            updateStatusAfterFill(sell);

            //KAFKA
            publishFillEventIfNeeded(buy, tradeQty);
            publishFillEventIfNeeded(sell, tradeQty);

            // Persist the resting order updates
            orderRepository.save(sell);
        }
    }

    private void matchSell(Order sell) {
        String ex = sell.getExchangeCode();

        // Get BUY candidates sorted by best price then FIFO
        List<Order> buys = orderRepository
                .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
                        ex,
                        sell.getInstrument(),
                        OrderSide.BUY,
                        ACTIVE_STATUSES,
                        0L
                );
        Long sellerId = sell.getUserId();

        for (Order buy : buys) {
            if (sell.getRemainingQuantity() <= 0) break;

            //  Prevent self-trade
            if (buy.getUserId().equals(sellerId)) {
                continue;
            }

            boolean sellIsMarket = sell.getType() != null && sell.getType().name().equals("MARKET");
            if (!sellIsMarket) {
                if (buy.getPrice().compareTo(sell.getPrice()) < 0) {
                    break;
                }
            }
            long tradeQty = Math.min(sell.getRemainingQuantity(), buy.getRemainingQuantity());

            // Trade price = resting order price (buy price)
            BigDecimal tradePrice = buy.getPrice();

            LocalDateTime now = LocalDateTime.now();

            // Publish trade event to Kafka
            TradeCreatedEvent event = tradeEventFactory.created(buy, sell, tradePrice, tradeQty, now);
            tradeEventsProducer.publish(String.valueOf(buy.getId()), event);

            // Update quantities
            sell.setRemainingQuantity(sell.getRemainingQuantity() - tradeQty);
            buy.setRemainingQuantity(buy.getRemainingQuantity() - tradeQty);

            // Update statuses
            updateStatusAfterFill(sell);
            updateStatusAfterFill(buy);

            publishFillEventIfNeeded(buy, tradeQty);
            publishFillEventIfNeeded(sell, tradeQty);
            // Persist the resting order updates
            orderRepository.save(buy);
        }
    }

    private void publishFillEventIfNeeded(Order order, long filledNow) {
        if (order.getStatus() == OrderStatus.PARTIALLY_FILLED) {
            var ev = orderEventFactory.partiallyFilled(order, filledNow);
            orderEventsProducer.publish(String.valueOf(order.getId()), ev);
        } else if (order.getStatus() == OrderStatus.FILLED) {
            var ev = orderEventFactory.filled(order);
            orderEventsProducer.publish(String.valueOf(order.getId()), ev);
        }
    }

    private void updateStatusAfterFill(Order order) {
        long rem = order.getRemainingQuantity();

        if (rem <= 0) {
            order.setRemainingQuantity(0L);
            order.setStatus(OrderStatus.FILLED);
        } else if (rem < order.getQuantity()) {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        } else {
            // nothing filled yet
            order.setStatus(OrderStatus.NEW);
        }
    }
}
