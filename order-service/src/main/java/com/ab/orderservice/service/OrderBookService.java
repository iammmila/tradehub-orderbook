package com.ab.orderservice.service;

import com.ab.orderservice.dto.orderbook.OrderBookResponse;
import com.ab.orderservice.mapper.OrderMapper;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderBookService {
    private final OrderRepository orderRepository;
    private final ExchangeRegistry exchangeRegistry;

    private static final List<OrderStatus> VISIBLE =
            List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED);

    public OrderBookResponse getOrderBook(String instrument, String exchange, boolean aggregated) {
        String inst = instrument.trim().toUpperCase(Locale.ROOT);
        if (aggregated) {
            return aggregatedBook(inst);
        }
        String ex = exchangeRegistry.normalizeOrDefault(exchange);
        return perExchangeBook(inst, ex);
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
}