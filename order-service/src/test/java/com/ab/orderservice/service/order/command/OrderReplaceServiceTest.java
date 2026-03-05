package com.ab.orderservice.service.order.command;

import com.ab.orderservice.dto.OrderResponse;
import com.ab.orderservice.dto.ReplaceOrderRequest;
import com.ab.orderservice.exception.BadRequestException;
import com.ab.orderservice.exception.ForbiddenException;
import com.ab.orderservice.exception.NotFoundException;
import com.ab.orderservice.kafka.OrderEventFactory;
import com.ab.orderservice.kafka.OrderEventsProducer;
import com.ab.orderservice.kafka.event.OrderReplacedEvent;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.repository.OrderRepository;
import com.ab.orderservice.service.order.support.OrderAccessChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderReplaceServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderAccessChecker accessChecker;
    @Mock
    private OrderEventFactory orderEventFactory;
    @Mock
    private OrderEventsProducer orderEventsProducer;

    @InjectMocks
    private OrderReplaceService service;

    @Test
    void replace_shouldThrowNotFound_whenMissing() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        ReplaceOrderRequest req = ReplaceOrderRequest.builder()
                .price(new BigDecimal("2.00"))
                .quantity(10L)
                .build();

        assertThatThrownBy(() -> service.replace(404L, 10L, false, req))
                .isInstanceOf(NotFoundException.class);

        verify(orderRepository, never()).save(any());
        verify(orderEventsProducer, never()).publish(anyString(), any());
    }

    @Test
    void replace_shouldThrowBadRequest_whenStatusNotNew() {
        Order order = Order.builder()
                .id(6L)
                .userId(10L)
                .status(OrderStatus.CANCELLED)
                .quantity(100L)
                .remainingQuantity(100L)
                .instrument("AAPL")
                .side(OrderSide.BUY)
                .price(new BigDecimal("1.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(6L)).thenReturn(Optional.of(order));
        doNothing().when(accessChecker).requireOwnerOrAdmin(order, 10L, false);

        ReplaceOrderRequest req = ReplaceOrderRequest.builder()
                .price(new BigDecimal("2.00"))
                .build();

        assertThatThrownBy(() -> service.replace(6L, 10L, false, req))
                .isInstanceOf(BadRequestException.class);

        verify(orderRepository, never()).save(any());
        verify(orderEventsProducer, never()).publish(anyString(), any());
    }

    @Test
    void replace_shouldUpdatePriceOnly_whenQuantityNull() {
        Order order = Order.builder()
                .id(8L)
                .userId(10L)
                .status(OrderStatus.NEW)
                .quantity(100L)
                .remainingQuantity(100L)
                .instrument("AAPL")
                .side(OrderSide.BUY)
                .price(new BigDecimal("1.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(8L)).thenReturn(Optional.of(order));
        doNothing().when(accessChecker).requireOwnerOrAdmin(order, 10L, false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderReplacedEvent replacedEvent = mock(OrderReplacedEvent.class);
        when(orderEventFactory.replaced(any(Order.class), any(Order.class))).thenReturn(replacedEvent);

        ReplaceOrderRequest req = ReplaceOrderRequest.builder()
                .price(new BigDecimal("2.50"))
                .build();

        // Act
        OrderResponse resp = service.replace(8L, 10L, false, req);

        // Only price changed.
        assertThat(resp.getPrice()).isEqualByComparingTo("2.50");
        assertThat(resp.getQuantity()).isEqualTo(100L);
        assertThat(resp.getRemainingQuantity()).isEqualTo(100L);

        verify(orderEventsProducer).publish(eq("8"), same(replacedEvent));
    }

    @Test
    void replace_shouldUpdateQuantityAndRemaining_basedOnFilled() {
        // oldQty=100, remaining=70 -> filled=30
        Order order = Order.builder()
                .id(9L)
                .userId(10L)
                .status(OrderStatus.NEW)
                .quantity(100L)
                .remainingQuantity(70L)
                .instrument("AAPL")
                .side(OrderSide.BUY)
                .price(new BigDecimal("1.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));
        doNothing().when(accessChecker).requireOwnerOrAdmin(order, 10L, false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderReplacedEvent replacedEvent = mock(OrderReplacedEvent.class);
        when(orderEventFactory.replaced(any(Order.class), any(Order.class))).thenReturn(replacedEvent);

        ReplaceOrderRequest req = ReplaceOrderRequest.builder()
                .quantity(120L) // newQty - filled(30) -> remaining 90
                .build();

        // Act
        OrderResponse resp = service.replace(9L, 10L, false, req);

        assertThat(resp.getQuantity()).isEqualTo(120L);
        assertThat(resp.getRemainingQuantity()).isEqualTo(90L);

        verify(orderEventsProducer).publish(eq("9"), same(replacedEvent));
    }

    @Test
    void replace_shouldReject_whenNewQuantityLessThanAlreadyFilled() {
        // filled=30
        Order order = Order.builder()
                .id(10L)
                .userId(10L)
                .status(OrderStatus.NEW)
                .quantity(100L)
                .remainingQuantity(70L)
                .instrument("AAPL")
                .side(OrderSide.BUY)
                .price(new BigDecimal("1.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        doNothing().when(accessChecker).requireOwnerOrAdmin(order, 10L, false);

        ReplaceOrderRequest req = ReplaceOrderRequest.builder()
                .quantity(20L) // < filled(30)
                .build();

        assertThatThrownBy(() -> service.replace(10L, 10L, false, req))
                .isInstanceOf(BadRequestException.class);

        verify(orderRepository, never()).save(any());
        verify(orderEventsProducer, never()).publish(anyString(), any());
    }
}