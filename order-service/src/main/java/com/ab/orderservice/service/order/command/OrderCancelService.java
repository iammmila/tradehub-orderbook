package com.ab.orderservice.service.order.command;

import com.ab.orderservice.exception.BadRequestException;
import com.ab.orderservice.exception.NotFoundException;
import com.ab.orderservice.exception.enums.ErrorCode;
import com.ab.orderservice.dto.OrderResponse;
import com.ab.orderservice.kafka.OrderEventFactory;
import com.ab.orderservice.kafka.OrderEventsProducer;
import com.ab.orderservice.kafka.event.OrderCancelledEvent;
import com.ab.orderservice.mapper.OrderMapper;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.repository.OrderRepository;
import com.ab.orderservice.service.order.support.OrderAccessChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderCancelService {
    private final OrderRepository orderRepository;
    private final OrderAccessChecker accessChecker;
    private final OrderEventFactory orderEventFactory;
    private final OrderEventsProducer orderEventsProducer;

    @Transactional
    public OrderResponse cancel(Long orderId, Long currentUserId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ORDER_NOT_FOUND));

        accessChecker.requireOwnerOrAdmin(order, currentUserId, isAdmin);

        // MVP rule: only NEW orders can be cancelled.
        if (order.getStatus() != OrderStatus.NEW) {
            throw new BadRequestException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        OrderCancelledEvent event = orderEventFactory.cancelled(saved, "USER_REQUEST");
        orderEventsProducer.publish(String.valueOf(saved.getId()), event);

        return OrderMapper.toResponse(saved);
    }
}