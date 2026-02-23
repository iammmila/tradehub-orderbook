package com.ab.orderservice.service;

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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchingService {
    private final OrderRepository orderRepository;
    private final TradeEventFactory tradeEventFactory;
    private final TradeEventsProducer tradeEventsProducer;

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
        String instrument = incoming.getInstrument().trim();
        incoming.setInstrument(instrument);

        if (incoming.getSide() == OrderSide.BUY) {
            matchBuy(incoming);
        } else {
            matchSell(incoming);
        }

        orderRepository.save(incoming);
    }

    private void matchBuy(Order buy) {
        // Get SELL candidates sorted by best price then FIFO
        List<Order> sells = orderRepository
                .findByInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
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

            // Price condition: buyPrice >= sellPrice
            if (buy.getPrice().compareTo(sell.getPrice()) < 0) {
                // Because sells are sorted by price ASC, once this fails, all next sells will be even more expensive
                break;
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

            // Persist the resting order updates
            orderRepository.save(sell);
        }
    }

    private void matchSell(Order sell) {
        // Get BUY candidates sorted by best price then FIFO
        List<Order> buys = orderRepository
                .findByInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
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
            // Price condition: buyPrice >= sellPrice
            if (buy.getPrice().compareTo(sell.getPrice()) < 0) {
                // Because buys are sorted by price DESC, once this fails, all next buys will be even lower
                break;
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

            // Persist the resting order updates
            orderRepository.save(buy);
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
