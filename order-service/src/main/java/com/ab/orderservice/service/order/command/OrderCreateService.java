package com.ab.orderservice.service.order.command;

import com.ab.orderservice.dto.CreateOrderRequest;
import com.ab.orderservice.dto.OrderResponse;
import com.ab.orderservice.kafka.OrderEventFactory;
import com.ab.orderservice.kafka.OrderEventsProducer;
import com.ab.orderservice.kafka.event.OrderCreatedEvent;
import com.ab.orderservice.mapper.OrderMapper;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.model.enums.OrderType;
import com.ab.orderservice.repository.OrderRepository;
import com.ab.orderservice.service.MatchingService;
import com.ab.orderservice.service.order.support.OrderAccessChecker;
import com.ab.orderservice.service.order.support.OrderRoutingService;
import com.ab.orderservice.service.order.support.OrderValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderCreateService {

    private final OrderRepository orderRepository;
    private final MatchingService matchingService;
    private final OrderEventsProducer orderEventsProducer;
    private final OrderEventFactory orderEventFactory;

    private final OrderAccessChecker accessChecker;
    private final OrderValidator validator;
    private final OrderRoutingService routingService;


    @Transactional
    public OrderResponse create(Long userId, CreateOrderRequest request) {
        // Ensures token belongs to user and account is verified.
        accessChecker.requireVerifiedSameUser(userId);

        OrderType type = validator.resolveType(request);
        boolean visible = type != OrderType.HIDDEN_LIMIT;

        String instrument = validator.normalizeInstrument(request.getInstrument());
        var price = validator.resolvePrice(type, request.getPrice());
        var minExecSize = validator.resolveMinExecSize(type, request.getMinExecSize(), request.getQuantity());

        var routing = routingService.resolve(
                request.getExchangeCode(),
                instrument,
                request.getSide(),
                type,
                type == OrderType.MARKET ? null : price,
                request.getQuantity()
        );

        Order order = Order.builder()
                .instrument(instrument)
                .exchangeCode(routing.exchangeCode())
                .side(request.getSide())
                .type(type)
                .visible(visible)
                .minExecSize(minExecSize)
                .price(price)
                .quantity(request.getQuantity())
                .remainingQuantity(request.getQuantity())
                .status(OrderStatus.NEW)
                .createdAt(LocalDateTime.now())
                .userId(userId)
                .routingMode(routing.routingMode())
                .routedBy(routing.routedBy())
                .routeReason(routing.routeReason())
                .build();

        Order saved = orderRepository.save(order);

        // Publishes ORDER_CREATED to notify other services (trade, notification, etc.).
        OrderCreatedEvent event = orderEventFactory.created(saved);
        orderEventsProducer.publish(String.valueOf(saved.getId()), event);

        // Triggers matching; order state may change (fills, status).
        matchingService.match(saved);

        Order updated = orderRepository.findById(saved.getId()).orElse(saved);
        return OrderMapper.toResponse(updated);
    }
}
