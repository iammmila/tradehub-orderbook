package com.ab.orderservice.service.order.command;

import com.ab.orderservice.dto.OrderResponse;
import com.ab.orderservice.exception.BadRequestException;
import com.ab.orderservice.exception.NotFoundException;
import com.ab.orderservice.kafka.OrderEventFactory;
import com.ab.orderservice.kafka.OrderEventsProducer;
import com.ab.orderservice.kafka.event.OrderCancelledEvent;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCancelServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderAccessChecker accessChecker;
    @Mock
    private OrderEventFactory orderEventFactory;
    @Mock
    private OrderEventsProducer orderEventsProducer;

    @InjectMocks
    private OrderCancelService service;

    @Test
    void cancel_shouldThrowNotFound_whenMissing() {
        when(orderRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(123L, 10L, false))
                .isInstanceOf(NotFoundException.class);

        verify(orderRepository, never()).save(any());
        verify(orderEventsProducer, never()).publish(anyString(), any());
    }

    @Test
    void cancel_shouldThrowBadRequest_whenStatusNotNew() {
        Order order = Order.builder()
                .id(2L)
                .userId(10L)
                .status(OrderStatus.FILLED)
                .remainingQuantity(0L)
                .quantity(10L)
                .instrument("AAPL")
                .side(OrderSide.BUY)
                .price(new BigDecimal("1.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));
        doNothing().when(accessChecker).requireOwnerOrAdmin(order, 10L, false);

        assertThatThrownBy(() -> service.cancel(2L, 10L, false))
                .isInstanceOf(BadRequestException.class);

        verify(orderRepository, never()).save(any());
        verify(orderEventsProducer, never()).publish(anyString(), any());
    }

    @Test
    void cancel_shouldSetCancelled_save_publish_andReturnResponse() {
        Order order = Order.builder()
                .id(3L)
                .userId(10L)
                .status(OrderStatus.NEW)
                .remainingQuantity(10L)
                .quantity(10L)
                .instrument("AAPL")
                .side(OrderSide.BUY)
                .price(new BigDecimal("1.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));
        doNothing().when(accessChecker).requireOwnerOrAdmin(order, 10L, false);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderCancelledEvent cancelledEvent = mock(OrderCancelledEvent.class);
        when(orderEventFactory.cancelled(any(Order.class), eq("USER_REQUEST"))).thenReturn(cancelledEvent);

        // Act
        OrderResponse resp = service.cancel(3L, 10L, false);

        // Status changed.
        assertThat(resp.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // Saved + published.
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.CANCELLED));
        verify(orderEventsProducer).publish(eq("3"), same(cancelledEvent));
    }
}
