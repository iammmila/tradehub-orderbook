package com.ab.orderservice.orders.service;

import com.ab.orderservice.common.exception.BadRequestException;
import com.ab.orderservice.common.exception.ForbiddenException;
import com.ab.orderservice.common.exception.enums.ErrorCode;
import com.ab.orderservice.common.exception.NotFoundException;
import com.ab.orderservice.orders.dto.CreateOrderRequest;
import com.ab.orderservice.orders.dto.OrderResponse;
import com.ab.orderservice.orders.dto.ReplaceOrderRequest;
import com.ab.orderservice.orders.mapper.OrderMapper;
import com.ab.orderservice.orders.model.Order;
import com.ab.orderservice.auth.model.User;
import com.ab.orderservice.orders.model.enums.OrderSide;
import com.ab.orderservice.orders.model.enums.OrderStatus;
import com.ab.orderservice.orders.repository.OrderRepository;
import com.ab.orderservice.auth.repository.UserRepository;
import com.ab.orderservice.orders.repository.OrderSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final MatchingService matchingService;

    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND));

        Order order = Order.builder()
                .instrument(request.getInstrument())
                .side(request.getSide())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .remainingQuantity(request.getQuantity())
                .status(OrderStatus.NEW)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        Order saved = orderRepository.save(order);

        //run matching after create
        matchingService.match(saved);

        // reload saved state (because matching may update it)
        Order updated = orderRepository.findById(saved.getId()).orElse(saved);
        return OrderMapper.toResponse(updated);
    }

    public List<OrderResponse> getOrders(OrderSide side, String instrument, OrderStatus status) {
        Specification<Order> specification = OrderSpecifications.withFilters(side, instrument, status);

        return orderRepository.findAll(specification)
                .stream()
                .map(OrderMapper::toResponse)
                .toList();
    }

    public Page<OrderResponse> getOrdersByUserPaged(
            Long userId,
            OrderSide side,
            String instrument,
            OrderStatus status,
            Pageable pageable
    ) {
        var specification = OrderSpecifications.byUserAndFilters(userId, side, instrument, status);
        return orderRepository
                .findAll(specification, pageable)
                .map(OrderMapper::toResponse);
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
        return OrderMapper.toResponse(saved);
    }

    public OrderResponse replaceOrder(
            Long orderId,
            Long currentUserId,
            boolean isAdmin,
            ReplaceOrderRequest request
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ORDER_NOT_FOUND));

        // Ownership check (unless ADMIN)
        Long ownerId = order.getUser().getId();
        if (!isAdmin && !ownerId.equals(currentUserId)) {
            throw new ForbiddenException(ErrorCode.ACCESS_DENIED);
        }

        // Only NEW orders can be replaced
        if (order.getStatus() != OrderStatus.NEW) {
            throw new BadRequestException(ErrorCode.ORDER_CANNOT_REPLACE);
        }

        // Decide new values
        if (request.getPrice() != null) {
            order.setPrice(request.getPrice());
        }

        if (request.getQuantity() != null) {
            long newQty = request.getQuantity();
            long oldQty = order.getQuantity();
            long oldRemaining = order.getRemainingQuantity();

            long filled = oldQty - oldRemaining;

            // cannot reduce quantity below already filled amount
            if (newQty < filled) {
                throw new BadRequestException(ErrorCode.ORDER_REPLACE_INVALID_QUANTITY);
            }

            order.setQuantity(newQty);
            order.setRemainingQuantity(newQty - filled);
        }

        Order saved = orderRepository.save(order);
        return OrderMapper.toResponse(saved);
    }
}
