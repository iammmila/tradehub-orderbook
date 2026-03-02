package com.ab.orderservice.service;

import com.ab.orderservice.dto.CreateOrderRequest;
import com.ab.orderservice.dto.OrderResponse;
import com.ab.orderservice.dto.ReplaceOrderRequest;
import com.ab.orderservice.exception.BadRequestException;
import com.ab.orderservice.exception.ForbiddenException;
import com.ab.orderservice.exception.NotFoundException;
import com.ab.orderservice.kafka.OrderEventFactory;
import com.ab.orderservice.kafka.OrderEventsProducer;
import com.ab.orderservice.kafka.event.OrderCancelledEvent;
import com.ab.orderservice.kafka.event.OrderCreatedEvent;
import com.ab.orderservice.kafka.event.OrderReplacedEvent;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.*;
import com.ab.orderservice.repository.OrderRepository;
import com.ab.orderservice.router.SmartOrderRouter;
import org.junit.jupiter.api.BeforeEach;
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

//Enables Mockito annotations. Faster tests.
@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MatchingService matchingService;

    @Mock
    private OrderEventsProducer orderEventsProducer;

    @Mock
    private OrderEventFactory orderEventFactory;

    @Mock
    private ExchangeRegistry exchangeRegistry;

    @Mock
    private SmartOrderRouter smartOrderRouter;

    @InjectMocks
    private OrderService orderService;


    private final Long userId = 10L;

    @BeforeEach
    void setup() {
        //!!! runs before each test.
        // Useful when you want to reset shared data or define common defaults.
        // In this test class it’s optional, but keeping it is fine for future shared setup.
    }

    @Test
    void createOrder_shouldSavePublishMatchReloadAndReturnResponse() {
        // Arrange
        CreateOrderRequest req = CreateOrderRequest.builder()
                .instrument("AAPL")
                .side(OrderSide.BUY)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("150.25"))
                .quantity(100L)
                .exchangeCode(null)
                .build();

        // Router returns a decision (AUTO)
        var decision = mock(com.ab.orderservice.router.RouteDecision.class);
        when(decision.getChosenExchange()).thenReturn("XNAS");
        when(decision.getReason()).thenReturn("BEST_PRICE");

        when(smartOrderRouter.route(
                eq("AAPL"),
                eq(OrderSide.BUY),
                eq(OrderType.LIMIT),
                eq(new BigDecimal("150.25")),
                eq(100L)
        )).thenReturn(decision);

        // normalizeOrDefault should return a valid exchange code
        when(exchangeRegistry.normalizeOrDefault("XNAS")).thenReturn("XNAS");

        // repository save assigns id
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(99L);
            return o;
        });

        OrderCreatedEvent createdEvent = mock(OrderCreatedEvent.class);
        when(orderEventFactory.created(any(Order.class))).thenReturn(createdEvent);

        // reload returns "updated" after matching
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
                .createdAt(LocalDateTime.now())
                .userId(userId)
                .routingMode(RoutingMode.AUTO)
                .routedBy(RoutedBy.SOR)
                .routeReason("BEST_PRICE")
                .build();

        when(orderRepository.findById(99L)).thenReturn(Optional.of(updated));

        // Act
        OrderResponse resp = orderService.createOrder(userId, req);

        // Assert saved order fields before matching
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
        assertThat(savedArg.getCreatedAt()).isNotNull();

        assertThat(savedArg.getRoutingMode()).isEqualTo(RoutingMode.AUTO);
        assertThat(savedArg.getRoutedBy()).isEqualTo(RoutedBy.SOR);
        assertThat(savedArg.getRouteReason()).isEqualTo("BEST_PRICE");

        // Kafka publish + match + reload
        verify(orderEventFactory).created(any(Order.class));
        verify(orderEventsProducer).publish(eq("99"), same(createdEvent));
        verify(matchingService).match(any(Order.class));
        verify(orderRepository).findById(99L);

        // Response reflects reloaded order
        assertThat(resp.getId()).isEqualTo(99L);
        assertThat(resp.getRemainingQuantity()).isEqualTo(40L);
        assertThat(resp.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);
    }

    @Test
    void createOrder_shouldReturnSavedIfReloadNotFound() {
        // Arrange
        CreateOrderRequest req = CreateOrderRequest.builder()
                .instrument("VOD")
                .side(OrderSide.SELL)
                .type(OrderType.LIMIT)
                .price(new BigDecimal("10.00"))
                .quantity(50L)
                .exchangeCode(null)
                .build();

        // Router decision
        var decision = mock(com.ab.orderservice.router.RouteDecision.class);
        when(decision.getChosenExchange()).thenReturn("XLON");
        when(decision.getReason()).thenReturn("BEST_PROCEEDS");

        when(smartOrderRouter.route(
                eq("VOD"),
                eq(OrderSide.SELL),
                eq(OrderType.LIMIT),
                eq(new BigDecimal("10.00")),
                eq(50L)
        )).thenReturn(decision);

        when(exchangeRegistry.normalizeOrDefault("XLON")).thenReturn("XLON");

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(7L);
            return o;
        });

        OrderCreatedEvent createdEvent = mock(OrderCreatedEvent.class);
        when(orderEventFactory.created(any(Order.class))).thenReturn(createdEvent);

        // Reload fails -> fallback to saved
        when(orderRepository.findById(7L)).thenReturn(Optional.empty());

        // Act
        OrderResponse resp = orderService.createOrder(userId, req);

        // Assert: publish + match happened
        verify(orderEventsProducer).publish(eq("7"), same(createdEvent));
        verify(matchingService).match(any(Order.class));
        verify(orderRepository).findById(7L);

        // Fallback response is based on "saved" order (NEW, remaining = quantity)
        assertThat(resp.getId()).isEqualTo(7L);
        assertThat(resp.getInstrument()).isEqualTo("VOD");
        assertThat(resp.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(resp.getRemainingQuantity()).isEqualTo(50L);
    }

    @Test
    void cancelOrder_shouldThrowNotFound_whenMissing() {
        // given
        // service should return 404-style exception when order does not exist
        when(orderRepository.findById(123L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> orderService.cancelOrder(123L, userId, false))
                .isInstanceOf(NotFoundException.class);

        verify(orderRepository, never()).save(any());
        verify(orderEventsProducer, never()).publish(anyString(), any());
    }

    @Test
    void cancelOrder_shouldThrowForbidden_whenNotOwnerAndNotAdmin() {
        // given
        // ownership rule -> only owner can cancel unless admin
        Order order = Order.builder()
                .id(1L)
                .userId(999L) // different owner
                .status(OrderStatus.NEW)
                .remainingQuantity(10L)
                .quantity(10L)
                .instrument("AAPL")
                .side(OrderSide.BUY)
                .price(new BigDecimal("1.00"))
                .createdAt(LocalDateTime.now())
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        // when / then
        assertThatThrownBy(() -> orderService.cancelOrder(1L, userId, false))
                .isInstanceOf(ForbiddenException.class);

        verify(orderRepository, never()).save(any());
        verify(orderEventsProducer, never()).publish(anyString(), any());
    }

    @Test
    void cancelOrder_shouldThrowBadRequest_whenStatusNotNew() {
        // given
        // business rule -> only NEW can be cancelled
        Order order = Order.builder()
                .id(2L)
                .userId(userId)
                .status(OrderStatus.FILLED) // not NEW
                .remainingQuantity(0L)
                .quantity(10L)
                .instrument("AAPL")
                .side(OrderSide.BUY)
                .price(new BigDecimal("1.00"))
                .createdAt(LocalDateTime.now())
                .build();
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));

        // when / then
        assertThatThrownBy(() -> orderService.cancelOrder(2L, userId, false))
                .isInstanceOf(BadRequestException.class);

        verify(orderRepository, never()).save(any());
        verify(orderEventsProducer, never()).publish(anyString(), any());
    }

    @Test
    void cancelOrder_shouldCancelAndPublishEvent_whenOwner() {
        // given
        // cancel scenario
        Order order = Order.builder()
                .id(3L)
                .userId(userId)
                .status(OrderStatus.NEW)
                .remainingQuantity(10L)
                .quantity(10L)
                .instrument("AAPL")
                .side(OrderSide.BUY)
                .price(new BigDecimal("1.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderCancelledEvent cancelledEvent = mock(OrderCancelledEvent.class);
        when(orderEventFactory.cancelled(any(Order.class), eq("USER_REQUEST"))).thenReturn(cancelledEvent);

        // when
        OrderResponse resp = orderService.cancelOrder(3L, userId, false);

        // then
        assertThat(resp.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.CANCELLED));
        verify(orderEventsProducer).publish(eq("3"), same(cancelledEvent));
    }

    @Test
    void replaceOrder_shouldThrowNotFound_whenMissing() {
        when(orderRepository.findById(404L)).thenReturn(Optional.empty());

        ReplaceOrderRequest req = ReplaceOrderRequest.builder()
                .price(new BigDecimal("2.00"))
                .quantity(10L)
                .build();

        assertThatThrownBy(() -> orderService.replaceOrder(404L, userId, false, req))
                .isInstanceOf(NotFoundException.class);

        verify(orderRepository, never()).save(any());
        verify(orderEventsProducer, never()).publish(anyString(), any());
    }

    @Test
    void replaceOrder_shouldThrowForbidden_whenNotOwnerAndNotAdmin() {
        Order order = Order.builder()
                .id(5L)
                .userId(999L)
                .status(OrderStatus.NEW)
                .quantity(100L)
                .remainingQuantity(80L)
                .instrument("AAPL")
                .side(OrderSide.BUY)
                .price(new BigDecimal("1.00"))
                .createdAt(LocalDateTime.now())
                .build();
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        ReplaceOrderRequest req = ReplaceOrderRequest.builder()
                .price(new BigDecimal("2.00"))
                .build();

        assertThatThrownBy(() -> orderService.replaceOrder(5L, userId, false, req))
                .isInstanceOf(ForbiddenException.class);

        verify(orderRepository, never()).save(any());
        verify(orderEventsProducer, never()).publish(anyString(), any());
    }

    @Test
    void replaceOrder_shouldThrowBadRequest_whenStatusNotNew() {
        Order order = Order.builder()
                .id(6L)
                .userId(userId)
                .status(OrderStatus.CANCELLED)
                .quantity(100L)
                .remainingQuantity(100L)
                .instrument("AAPL")
                .side(OrderSide.BUY)
                .price(new BigDecimal("1.00"))
                .createdAt(LocalDateTime.now())
                .build();
        when(orderRepository.findById(6L)).thenReturn(Optional.of(order));

        ReplaceOrderRequest req = ReplaceOrderRequest.builder()
                .price(new BigDecimal("2.00"))
                .build();

        assertThatThrownBy(() -> orderService.replaceOrder(6L, userId, false, req))
                .isInstanceOf(BadRequestException.class);

        verify(orderRepository, never()).save(any());
        verify(orderEventsProducer, never()).publish(anyString(), any());
    }


    @Test
    void replaceOrder_shouldUpdatePriceOnly_whenQuantityNull() {
        Order order = Order.builder()
                .id(8L)
                .userId(userId)
                .status(OrderStatus.NEW)
                .quantity(100L)
                .remainingQuantity(100L)
                .instrument("AAPL")
                .side(OrderSide.BUY)
                .price(new BigDecimal("1.00"))
                .createdAt(LocalDateTime.now())
                .build();
        when(orderRepository.findById(8L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderReplacedEvent replacedEvent = mock(OrderReplacedEvent.class);
        when(orderEventFactory.replaced(any(Order.class), any(Order.class))).thenReturn(replacedEvent);

        ReplaceOrderRequest req = ReplaceOrderRequest.builder()
                .price(new BigDecimal("2.50"))
                .build();

        OrderResponse resp = orderService.replaceOrder(8L, userId, false, req);

        assertThat(resp.getPrice()).isEqualByComparingTo("2.50");
        assertThat(resp.getQuantity()).isEqualTo(100L);
        assertThat(resp.getRemainingQuantity()).isEqualTo(100L);

        verify(orderEventsProducer).publish(eq("8"), same(replacedEvent));
    }

    @Test
    void replaceOrder_shouldUpdateQuantityAndRemaining_basedOnFilled() {
        // oldQty=100, remaining=70 -> filled=30
        Order order = Order.builder()
                .id(9L)
                .userId(userId)
                .status(OrderStatus.NEW)
                .quantity(100L)
                .remainingQuantity(70L)
                .instrument("AAPL")
                .side(OrderSide.BUY)
                .price(new BigDecimal("1.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderReplacedEvent replacedEvent = mock(OrderReplacedEvent.class);
        when(orderEventFactory.replaced(any(Order.class), any(Order.class))).thenReturn(replacedEvent);

        ReplaceOrderRequest req = ReplaceOrderRequest.builder()
                .quantity(120L) // newQty - filled(30) -> remaining 90
                .build();

        OrderResponse resp = orderService.replaceOrder(9L, userId, false, req);

        assertThat(resp.getQuantity()).isEqualTo(120L);
        assertThat(resp.getRemainingQuantity()).isEqualTo(90L);

        verify(orderEventsProducer).publish(eq("9"), same(replacedEvent));
    }
}
