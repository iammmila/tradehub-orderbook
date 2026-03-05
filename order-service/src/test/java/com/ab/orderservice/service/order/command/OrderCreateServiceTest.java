package com.ab.orderservice.service.order.command;

import com.ab.orderservice.dto.CreateOrderRequest;
import com.ab.orderservice.dto.OrderResponse;
import com.ab.orderservice.kafka.OrderEventFactory;
import com.ab.orderservice.kafka.OrderEventsProducer;
import com.ab.orderservice.kafka.event.OrderCreatedEvent;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.*;
import com.ab.orderservice.repository.OrderRepository;
import com.ab.orderservice.service.MatchingService;
import com.ab.orderservice.service.order.support.OrderAccessChecker;
import com.ab.orderservice.service.order.support.OrderRoutingService;
import com.ab.orderservice.service.order.support.OrderValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderCreateServiceTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private MatchingService matchingService;
    @Mock
    private OrderEventsProducer orderEventsProducer;
    @Mock
    private OrderEventFactory orderEventFactory;

    @Mock
    private OrderAccessChecker accessChecker;
    @Mock
    private OrderValidator validator;
    @Mock
    private OrderRoutingService routingService;

    @InjectMocks
    private OrderCreateService service;

    @Test
    void create_shouldSavePublishMatchReload_andReturnUpdatedResponse() {
        Long userId = 10L;

        CreateOrderRequest req = CreateOrderRequest.builder()
                .instrument("aapl")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("150.25"))
                .quantity(100L)
                .exchangeCode(null)
                .minExecSize(null)
                .build();

        // Access rule check (verified + same user).
        doNothing().when(accessChecker).requireVerifiedSameUser(userId);

        // Request normalization and defaults.
        when(validator.resolveType(req)).thenReturn(OrderType.LIMIT);
        when(validator.normalizeInstrument("aapl")).thenReturn("AAPL");
        when(validator.resolvePrice(OrderType.LIMIT, new BigDecimal("150.25"))).thenReturn(new BigDecimal("150.25"));

        // Routing decision for exchange/mode labels.
        var routing = OrderRoutingService.RoutingDecision.auto("XNAS", "BEST_PRICE");
        when(routingService.resolve(
                isNull(),
                eq("AAPL"),
                eq(OrderSide.BUY),
                eq(OrderType.LIMIT),
                eq(new BigDecimal("150.25")),
                eq(100L)
        )).thenReturn(routing);

        // Repository assigns id.
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(99L);
            return o;
        });

        OrderCreatedEvent createdEvent = mock(OrderCreatedEvent.class);
        when(orderEventFactory.created(any(Order.class))).thenReturn(createdEvent);

        // Reload after matching returns updated state.
        Order updated = Order.builder()
                .id(99L)
                .instrument("AAPL")
                .exchangeCode("XNAS")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .visible(true)
                .price(new BigDecimal("150.25"))
                .quantity(100L)
                .remainingQuantity(40L)
                .status(OrderStatus.PARTIALLY_FILLED)
                .userId(userId)
                .routingMode(RoutingMode.AUTO)
                .routedBy(RoutedBy.SOR)
                .routeReason("BEST_PRICE")
                .build();

        when(orderRepository.findById(99L)).thenReturn(Optional.of(updated));

        // Act
        OrderResponse resp = service.create(userId, req);

        // Assert saved order fields (pre-match).
        ArgumentCaptor<Order> savedCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(savedCaptor.capture());
        Order savedArg = savedCaptor.getValue();

        assertThat(savedArg.getInstrument()).isEqualTo("AAPL");
        assertThat(savedArg.getExchangeCode()).isEqualTo("XNAS");
        assertThat(savedArg.getSide()).isEqualTo(OrderSide.BUY);
        assertThat(savedArg.getType()).isEqualTo(OrderType.LIMIT);
        assertThat(savedArg.getVisible()).isTrue();
        assertThat(savedArg.getPrice()).isEqualByComparingTo("150.25");
        assertThat(savedArg.getQuantity()).isEqualTo(100L);
        assertThat(savedArg.getRemainingQuantity()).isEqualTo(100L);
        assertThat(savedArg.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(savedArg.getUserId()).isEqualTo(userId);

        assertThat(savedArg.getRoutingMode()).isEqualTo(RoutingMode.AUTO);
        assertThat(savedArg.getRoutedBy()).isEqualTo(RoutedBy.SOR);
        assertThat(savedArg.getRouteReason()).isEqualTo("BEST_PRICE");

        // Publish + match + reload.
        verify(orderEventFactory).created(any(Order.class));
        verify(orderEventsProducer).publish(eq("99"), same(createdEvent));
        verify(matchingService).match(any(Order.class));
        verify(orderRepository).findById(99L);

        // Response from reloaded order.
        assertThat(resp.getId()).isEqualTo(99L);
        assertThat(resp.getRemainingQuantity()).isEqualTo(40L);
        assertThat(resp.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
    }

    @Test
    void create_shouldReturnSaved_whenReloadMissing() {
        Long userId = 10L;

        CreateOrderRequest req = CreateOrderRequest.builder()
                .instrument("VOD")
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("10.00"))
                .quantity(50L)
                .exchangeCode(null)
                .build();

        doNothing().when(accessChecker).requireVerifiedSameUser(userId);
        when(validator.resolveType(req)).thenReturn(OrderType.LIMIT);
        when(validator.normalizeInstrument("VOD")).thenReturn("VOD");
        when(validator.resolvePrice(OrderType.LIMIT, new BigDecimal("10.00"))).thenReturn(new BigDecimal("10.00"));

        var routing = OrderRoutingService.RoutingDecision.auto("XLON", "BEST_PROCEEDS");
        when(routingService.resolve(
                isNull(),
                eq("VOD"),
                eq(OrderSide.SELL),
                eq(OrderType.LIMIT),
                eq(new BigDecimal("10.00")),
                eq(50L)
        )).thenReturn(routing);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(7L);
            return o;
        });

        OrderCreatedEvent createdEvent = mock(OrderCreatedEvent.class);
        when(orderEventFactory.created(any(Order.class))).thenReturn(createdEvent);

        // Reload not found -> fallback to saved.
        when(orderRepository.findById(7L)).thenReturn(Optional.empty());

        // Act
        OrderResponse resp = service.create(userId, req);

        // Publish + match happened.
        verify(orderEventsProducer).publish(eq("7"), same(createdEvent));
        verify(matchingService).match(any(Order.class));
        verify(orderRepository).findById(7L);

        // Fallback response uses saved order state (NEW, remaining = quantity).
        assertThat(resp.getId()).isEqualTo(7L);
        assertThat(resp.getInstrument()).isEqualTo("VOD");
        assertThat(resp.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(resp.getRemainingQuantity()).isEqualTo(50L);
    }
}
