package com.ab.orderservice.service;

import com.ab.orderservice.dto.OrderResponse;
import com.ab.orderservice.dto.orderbook.OrderBookLevel;
import com.ab.orderservice.dto.orderbook.OrderBookResponse;
import com.ab.orderservice.exception.BadRequestException;
import com.ab.orderservice.exception.enums.ErrorCode;
import com.ab.orderservice.mapper.OrderMapper;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderBookService {
    private final OrderRepository orderRepository;
    private final ExchangeRegistry exchangeRegistry;

    private static final List<OrderStatus> VISIBLE =
            List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED);

    public OrderBookResponse getOrderBook(String instrument, String exchange, boolean aggregated, boolean levels) {
        String inst = instrument.trim().toUpperCase(Locale.ROOT);
        if (exchange != null && !exchange.isBlank() && !exchangeRegistry.isSupported(exchange)) {
            throw new BadRequestException(ErrorCode.EXCHANGE_NOT_SUPPORTED);
        }
        if (aggregated) {
            OrderBookResponse merged = aggregatedBook(inst);
            if (levels) {
                applyLevels(merged);
            }
            merged.setLevels(levels);
            return merged;
        }
        String ex = exchangeRegistry.normalizeOrDefault(exchange);
        OrderBookResponse perEx = perExchangeBook(inst, ex);
        perEx.setLevels(false);
        return perEx;
    }

    private OrderBookResponse perExchangeBook(String instrument, String exchangeCode) {
        List<Order> bids = orderRepository
                .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
                        exchangeCode, instrument, OrderSide.BUY, VISIBLE, 0L
                );

        List<Order> asks = orderRepository
                .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                        exchangeCode, instrument, OrderSide.SELL, VISIBLE, 0L
                );

        return OrderBookResponse.builder()
                .instrument(instrument)
                .exchange(exchangeCode)
                .aggregated(false)
                .bids(bids
                        .stream()
                        .map(OrderMapper::toResponse)
                        .toList())
                .asks(asks
                        .stream()
                        .map(OrderMapper::toResponse)
                        .toList())
                .build();
    }

    private OrderBookResponse aggregatedBook(String instrument) {
        List<Order> allBids = new ArrayList<>();
        List<Order> allAsks = new ArrayList<>();

        for (String ex : exchangeRegistry.codes()) {
            allBids.addAll(orderRepository
                    .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
                            ex, instrument, OrderSide.BUY, VISIBLE, 0L
                    ));
            allAsks.addAll(orderRepository
                    .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                            ex, instrument, OrderSide.SELL, VISIBLE, 0L
                    ));
        }
        allBids.sort(Comparator.comparing(Order::getPrice).reversed().thenComparing(Order::getCreatedAt));
        allAsks.sort(Comparator.comparing(Order::getPrice).thenComparing(Order::getCreatedAt));
        return OrderBookResponse.builder()
                .instrument(instrument)
                .exchange(null)
                .aggregated(true)
                .bids(allBids
                        .stream()
                        .map(OrderMapper::toResponse).toList()
                )
                .asks(allAsks
                        .stream()
                        .map(OrderMapper::toResponse)
                        .toList())
                .build();
    }

    public List<OrderBookLevel> getLevelsForExchange(String instrument, String exchange, boolean isBid) {
        OrderBookResponse book = perExchangeBook(
                instrument.trim().toUpperCase(Locale.ROOT),
                exchangeRegistry.normalizeOrDefault(exchange)
        );

        if (isBid) {
            return toLevels(book.getBids(), true);
        } else {
            return toLevels(book.getAsks(), false);
        }
    }

    private void applyLevels(OrderBookResponse book) {
        // Group by price, sum remainingQuantity
        List<OrderBookLevel> bidLvls = toLevels(book.getBids(), true);
        List<OrderBookLevel> askLvls = toLevels(book.getAsks(), false);

        book.setBidLevels(bidLvls);
        book.setAskLevels(askLvls);
    }

    private List<OrderBookLevel> toLevels(List<OrderResponse> orders, boolean isBid) {
        if (orders == null || orders.isEmpty()) return List.of();

        // Best practice: normalize BigDecimal scale so grouping works reliably
        Map<BigDecimal, Long> sums = new HashMap<>();

        for (OrderResponse o : orders) {
            if (o == null) continue;
            if (o.getPrice() == null) continue;

            long rem = (o.getRemainingQuantity() == null) ? 0L : o.getRemainingQuantity();
            if (rem <= 0) continue;

            BigDecimal p = normalizePrice(o.getPrice());
            sums.merge(p, rem, Long::sum);
        }

        Comparator<OrderBookLevel> cmp = Comparator.comparing(OrderBookLevel::getPrice);
        if (isBid) cmp = cmp.reversed(); // bids desc, asks asc

        return sums.entrySet().stream()
                .map(e -> OrderBookLevel.builder()
                        .price(e.getKey())
                        .totalQuantity(e.getValue())
                        .build())
                .sorted(cmp)
                .collect(Collectors.toList());
    }

    private BigDecimal normalizePrice(BigDecimal p) {
        return p.setScale(4, RoundingMode.HALF_UP);
    }
}