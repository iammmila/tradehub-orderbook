package com.ab.orderservice.mapper;

import com.ab.orderservice.dto.OrderResponse;
import com.ab.orderservice.model.Order;

public final class OrderMapper {

    private OrderMapper() {
    }

    // Maps internal Order entity to API response DTO.
    public static OrderResponse toResponse(Order order) {
        if (order == null) return null;

        return OrderResponse.builder()
                .id(order.getId())
                .instrument(order.getInstrument())
                .side(order.getSide())
                .price(order.getPrice())
                .quantity(order.getQuantity())
                .remainingQuantity(order.getRemainingQuantity())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .exchangeCode(order.getExchangeCode())
                .type(order.getType())
                .minExecSize(order.getMinExecSize())
                .visible(order.getVisible())
                .userId(order.getUserId())
                .routingMode(order.getRoutingMode())
                .routedBy(order.getRoutedBy())
                .routeReason(order.getRouteReason())
                .build();
    }

    // Creates an immutable snapshot for Kafka events (before/after comparison).
    public static Order snapshot(Order order) {
        if (order == null) return null;

        return Order.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .exchangeCode(order.getExchangeCode())
                .instrument(order.getInstrument())
                .type(order.getType())
                .visible(order.getVisible())
                .minExecSize(order.getMinExecSize())
                .side(order.getSide())
                .price(order.getPrice())
                .quantity(order.getQuantity())
                .remainingQuantity(order.getRemainingQuantity())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .routingMode(order.getRoutingMode())
                .routedBy(order.getRoutedBy())
                .routeReason(order.getRouteReason())
                .build();
    }
}
