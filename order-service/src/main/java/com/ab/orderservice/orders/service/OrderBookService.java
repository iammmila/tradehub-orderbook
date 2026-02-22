package com.ab.orderservice.orders.service;

import com.ab.orderservice.orders.dto.orderbook.OrderBookResponse;
import com.ab.orderservice.orders.mapper.OrderMapper;
import com.ab.orderservice.orders.model.Order;
import com.ab.orderservice.orders.model.enums.OrderSide;
import com.ab.orderservice.orders.model.enums.OrderStatus;
import com.ab.orderservice.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderBookService {
    private final OrderRepository orderRepository;

    public OrderBookResponse getOrderBook(String instrument) {
        String inst = instrument.trim();

        List<Order> bids = orderRepository
                .findByInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
                        inst,
                        OrderSide.BUY,
                        List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED), 0L
                );

        List<Order> asks = orderRepository
                .findByInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                        inst,
                        OrderSide.SELL,
                        List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED), 0L
                );

        return OrderBookResponse.builder()
                .instrument(inst)
                .bids(bids
                        .stream()
                        .map(OrderMapper::toResponse)
                        .toList()
                )
                .asks(asks
                        .stream()
                        .map(OrderMapper::toResponse)
                        .toList())
                .build();
    }
}
