package com.ab.orderservice.mapper;

import com.ab.orderservice.dto.OrderResponse;
import com.ab.orderservice.model.Order;

public final class OrderMapper {

    private OrderMapper() {
    }

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
}
