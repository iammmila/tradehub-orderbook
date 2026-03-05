package com.ab.orderservice.service.order.query;

import com.ab.orderservice.dto.OrderResponse;
import com.ab.orderservice.exception.NotFoundException;
import com.ab.orderservice.exception.enums.ErrorCode;
import com.ab.orderservice.mapper.OrderMapper;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.repository.OrderRepository;
import com.ab.orderservice.repository.OrderSpecifications;
import com.ab.orderservice.service.order.support.OrderAccessChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderQueryService {
    private final OrderRepository orderRepository;
    private final OrderAccessChecker accessChecker;

    @Transactional(readOnly = true)
    public OrderResponse getById(Long orderId, Long currentUserId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ORDER_NOT_FOUND));

        accessChecker.requireOwnerOrAdmin(order, currentUserId, isAdmin);
        return OrderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list(OrderSide side, String instrument, OrderStatus status) {
        Specification<Order> specification = OrderSpecifications.withFilters(side, instrument, status);

        return orderRepository.findAll(specification)
                .stream()
                .map(OrderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listByUserPaged(
            Long userId,
            OrderSide side,
            String instrument,
            OrderStatus status,
            Pageable pageable
    ) {
        var specification = OrderSpecifications.byUserAndFilters(userId, side, instrument, status);
        return orderRepository.findAll(specification, pageable).map(OrderMapper::toResponse);
    }
}