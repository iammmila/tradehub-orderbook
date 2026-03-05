package com.ab.orderservice.service.order.command;

import com.ab.orderservice.dto.OrderResponse;
import com.ab.orderservice.dto.ReplaceOrderRequest;
import com.ab.orderservice.exception.BadRequestException;
import com.ab.orderservice.exception.NotFoundException;
import com.ab.orderservice.exception.enums.ErrorCode;
import com.ab.orderservice.kafka.OrderEventFactory;
import com.ab.orderservice.kafka.OrderEventsProducer;
import com.ab.orderservice.kafka.event.OrderReplacedEvent;
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
public class OrderReplaceService {
    private final OrderRepository orderRepository;
    private final OrderAccessChecker accessChecker;
    private final OrderEventFactory orderEventFactory;
    private final OrderEventsProducer orderEventsProducer;

    @Transactional
    public OrderResponse replace(Long orderId, Long currentUserId, boolean isAdmin, ReplaceOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ORDER_NOT_FOUND));

        accessChecker.requireOwnerOrAdmin(order, currentUserId, isAdmin);

        if (order.getStatus() != OrderStatus.NEW) {
            throw new BadRequestException(ErrorCode.ORDER_CANNOT_REPLACE);
        }

        // Snapshot before changes for ORDER_REPLACED event.
        Order before = OrderMapper.snapshot(order);

        if (request.getPrice() != null) {
            order.setPrice(request.getPrice());
        }

        if (request.getQuantity() != null) {
            long newQty = request.getQuantity();
            long oldQty = order.getQuantity();
            long oldRemaining = order.getRemainingQuantity();

            long filled = oldQty - oldRemaining;
            if (newQty < filled) {
                throw new BadRequestException(ErrorCode.ORDER_REPLACE_INVALID_QUANTITY);
            }

            order.setQuantity(newQty);
            order.setRemainingQuantity(newQty - filled);
        }

        Order saved = orderRepository.save(order);

        OrderReplacedEvent event = orderEventFactory.replaced(before, saved);
        orderEventsProducer.publish(String.valueOf(saved.getId()), event);

        return OrderMapper.toResponse(saved);
    }
}
