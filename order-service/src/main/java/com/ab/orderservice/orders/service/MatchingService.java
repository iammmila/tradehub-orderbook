package com.ab.orderservice.orders.service;

import com.ab.orderservice.orders.model.Order;
import com.ab.orderservice.orders.model.enums.OrderSide;
import com.ab.orderservice.orders.model.enums.OrderStatus;
import com.ab.orderservice.orders.repository.OrderRepository;
import com.ab.orderservice.trades.model.Trade;
import com.ab.orderservice.trades.repository.TradeRepository;
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
    private final TradeRepository tradeRepository;

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
        Long buyerId = buy.getUser().getId();
        for (Order sell : sells) {
            if (buy.getRemainingQuantity() <= 0) break;

            //  Prevent self-trade
            if (sell.getUser().getId().equals(buyerId)) {
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

            // Create trade
            Trade trade = Trade.builder()
                    .instrument(buy.getInstrument())
                    .price(tradePrice)
                    .quantity(tradeQty)
                    .buyOrder(buy)
                    .sellOrder(sell)
                    .createdAt(LocalDateTime.now())
                    .build();
            tradeRepository.save(trade);

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
        Long sellerId = sell.getUser().getId();
        for (Order buy : buys) {
            if (sell.getRemainingQuantity() <= 0) break;

            //  Prevent self-trade
            if (buy.getUser().getId().equals(sellerId)) {
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

            // Create trade
            Trade trade = Trade.builder()
                    .instrument(sell.getInstrument())
                    .price(tradePrice)
                    .quantity(tradeQty)
                    .buyOrder(buy)
                    .sellOrder(sell)
                    .createdAt(LocalDateTime.now())
                    .build();
            tradeRepository.save(trade);

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
