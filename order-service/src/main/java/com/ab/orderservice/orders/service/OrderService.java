package com.ab.orderservice.orders.service;

import com.ab.orderservice.common.exception.BadRequestException;
import com.ab.orderservice.common.exception.ForbiddenException;
import com.ab.orderservice.common.exception.enums.ErrorCode;
import com.ab.orderservice.common.exception.NotFoundException;
import com.ab.orderservice.orders.dto.order.CreateOrderRequest;
import com.ab.orderservice.orders.dto.order.OrderResponse;
import com.ab.orderservice.orders.model.Order;
import com.ab.orderservice.auth.model.User;
import com.ab.orderservice.orders.model.enums.OrderSide;
import com.ab.orderservice.orders.model.enums.OrderStatus;
import com.ab.orderservice.orders.repository.OrderRepository;
import com.ab.orderservice.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        Order order = Order.builder()
                .instrument(request.getInstrument().trim())
                .side(request.getSide())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .remainingQuantity(request.getQuantity())
                .status(OrderStatus.NEW)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    public List<OrderResponse> getOrders(OrderSide side, String instrument, OrderStatus status) {
        String inst = (instrument == null || instrument.isBlank()) ? null : instrument.trim();

        List<Order> orders;

        if (side != null && inst != null && status != null) {
            orders = orderRepository.findBySideAndInstrumentAndStatus(side, inst, status);
        } else if (side != null && inst != null) {
            orders = orderRepository.findBySideAndInstrument(side, inst);
        } else if (side != null && status != null) {
            orders = orderRepository.findBySideAndStatus(side, status);
        } else if (inst != null && status != null) {
            orders = orderRepository.findByInstrumentAndStatus(inst, status);
        } else if (side != null) {
            orders = orderRepository.findBySide(side);
        } else if (inst != null) {
            orders = orderRepository.findByInstrument(inst);
        } else if (status != null) {
            orders = orderRepository.findByStatus(status);
        } else {
            orders = orderRepository.findAll();
        }

        return orders.stream().map(this::toResponse).toList();
    }

    public List<OrderResponse> getOrdersByUser(Long userId, OrderSide side, String instrument, OrderStatus status) {
        // ensure user exists (better API behavior)
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        String inst = (instrument == null || instrument.isBlank()) ? null : instrument.trim();

        List<Order> orders;

        if (side != null && inst != null && status != null) {
            orders = orderRepository.findByUser_IdAndSideAndInstrumentAndStatus(userId, side, inst, status);
        } else if (side != null && inst != null) {
            orders = orderRepository.findByUser_IdAndSideAndInstrument(userId, side, inst);
        } else if (side != null && status != null) {
            orders = orderRepository.findByUser_IdAndSideAndStatus(userId, side, status);
        } else if (inst != null && status != null) {
            orders = orderRepository.findByUser_IdAndInstrumentAndStatus(userId, inst, status);
        } else if (side != null) {
            orders = orderRepository.findByUser_IdAndSide(userId, side);
        } else if (inst != null) {
            orders = orderRepository.findByUser_IdAndInstrument(userId, inst);
        } else if (status != null) {
            orders = orderRepository.findByUser_IdAndStatus(userId, status);
        } else {
            orders = orderRepository.findByUser_Id(userId);
        }

        return orders.stream().map(this::toResponse).toList();
    }

    public OrderResponse cancelOrder(Long orderId, Long currentUserId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ORDER_NOT_FOUND));

        // Ownership check (unless ADMIN)
        Long ownerId = order.getUser().getId();
        if (!isAdmin && !ownerId.equals(currentUserId)) {
            throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
        }

        // For MVP: only NEW can be cancelled
        if (order.getStatus() != OrderStatus.NEW) {
            throw new BadRequestException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .instrument(order.getInstrument().trim().toUpperCase())
                .side(order.getSide())
                .price(order.getPrice())
                .quantity(order.getQuantity())
                .remainingQuantity(order.getRemainingQuantity())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .userId(order.getUser() != null ? order.getUser().getId() : null)
                .build();
    }
}
