package com.ab.orderservice.service;

import com.ab.orderservice.kafka.OrderEventFactory;
import com.ab.orderservice.kafka.OrderEventsProducer;
import com.ab.orderservice.kafka.TradeEventFactory;
import com.ab.orderservice.kafka.TradeEventsProducer;
import com.ab.orderservice.kafka.event.TradeCreatedEvent;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.model.enums.OrderType;
import com.ab.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
//unit tests, fast & isolated.
public class MatchingServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TradeEventFactory tradeEventFactory;

    @Mock
    private TradeEventsProducer tradeEventsProducer;

    @Mock
    private OrderEventFactory orderEventFactory;

    @Mock
    private OrderEventsProducer orderEventsProducer;

    @InjectMocks
    private MatchingService matchingService;

    // Helper to quickly build orders with defaults
    private Order order(Long id, Long userId, String instrument, OrderSide side,
                        String exchangeCode, OrderType type,
                        String price, long qty, long remaining, OrderStatus status) {
        return Order.builder()
                .id(id)
                .userId(userId)
                .instrument(instrument)
                .exchangeCode(exchangeCode)
                .type(type)
                .side(side)
                .price(new BigDecimal(price))
                .quantity(qty)
                .remainingQuantity(remaining)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void match_shouldReturnImmediately_whenRemainingQuantityNull() {
        Order incoming = order(
                1L, 10L, "AAPL", OrderSide.BUY,
                "XNAS", OrderType.LIMIT,
                "100.00", 10, 0, OrderStatus.NEW
        );
        incoming.setRemainingQuantity(null);

        matchingService.match(incoming);

        verifyNoInteractions(orderRepository, tradeEventFactory, tradeEventsProducer, orderEventFactory, orderEventsProducer);
    }

    @Test
    void match_shouldReturnImmediately_whenRemainingQuantityZeroOrNegative() {
        Order incoming = order(
                1L, 10L, "AAPL", OrderSide.BUY,
                "XNAS", OrderType.LIMIT,
                "100.00", 10, 0, OrderStatus.NEW
        );

        matchingService.match(incoming);

        verifyNoInteractions(orderRepository, tradeEventFactory, tradeEventsProducer, orderEventFactory, orderEventsProducer);
    }

    @Test
    void match_shouldReturnImmediately_whenStatusNotActive() {
        Order incoming = order(
                1L, 10L, "AAPL", OrderSide.BUY,
                "XNAS", OrderType.LIMIT,
                "100.00", 10, 10, OrderStatus.CANCELLED
        );

        matchingService.match(incoming);

        verifyNoInteractions(orderRepository, tradeEventFactory, tradeEventsProducer, orderEventFactory, orderEventsProducer);
    }

    @Test
    void match_shouldThrow_whenExchangeCodeMissing() {
        Order incoming = order(
                1L, 10L, "AAPL", OrderSide.BUY,
                null, OrderType.LIMIT,
                "100.00", 10, 10, OrderStatus.NEW
        );

        assertThatThrownBy(() -> matchingService.match(incoming))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exchangeCode");

        verifyNoInteractions(tradeEventFactory, tradeEventsProducer, orderEventFactory, orderEventsProducer);
        // orderRepository isn't called because we fail before querying
        verifyNoInteractions(orderRepository);
    }


    // BUY matching
    @Test
    void matchBuy_shouldFillCompletely_againstOneSell_andPublishTrade_andPersistBoth() {
        // incoming BUY: wants 10 at 105
        Order buy = order(
                100L, 10L, " AAPL ", OrderSide.BUY,
                "xnas", OrderType.LIMIT,
                "105.00", 10, 10, OrderStatus.NEW
        );

        // resting SELL: 10 at 100
        Order sell = order(
                200L, 20L, "AAPL", OrderSide.SELL,
                "XNAS", OrderType.LIMIT,
                "100.00", 10, 10, OrderStatus.NEW
        );

        when(orderRepository
                .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                        eq("XNAS"), eq("AAPL"), eq(OrderSide.SELL), anyList(), eq(0L)
                )
        ).thenReturn(List.of(sell));

        TradeCreatedEvent tradeEvent = mock(TradeCreatedEvent.class);
        when(tradeEventFactory.created(any(Order.class), any(Order.class), any(BigDecimal.class), anyLong(), any(LocalDateTime.class)))
                .thenReturn(tradeEvent);

        // Fill events (your service publishes these now)
        when(orderEventFactory.filled(any(Order.class))).thenReturn(mock(com.ab.orderservice.kafka.event.OrderFilledEvent.class));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        matchingService.match(buy);

        // instrument + exchangeCode normalized
        assertThat(buy.getInstrument()).isEqualTo("AAPL");
        assertThat(buy.getExchangeCode()).isEqualTo("XNAS");

        // both FILLED
        assertThat(buy.getRemainingQuantity()).isEqualTo(0L);
        assertThat(buy.getStatus()).isEqualTo(OrderStatus.FILLED);

        assertThat(sell.getRemainingQuantity()).isEqualTo(0L);
        assertThat(sell.getStatus()).isEqualTo(OrderStatus.FILLED);

        // Trade publish uses buy id as key
        verify(tradeEventsProducer).publish(eq("100"), same(tradeEvent));

        // Filled events published for both orders
        verify(orderEventsProducer).publish(eq("100"), any()); // buy filled
        verify(orderEventsProducer).publish(eq("200"), any()); // sell filled

        // Persist resting order during loop + persist incoming at end
        verify(orderRepository, atLeastOnce()).save(sell);
        verify(orderRepository, atLeastOnce()).save(buy);
    }

    @Test
    void matchBuy_shouldPartiallyFill_whenSellNotEnough() {
        Order buy = order(
                101L, 10L, "AAPL", OrderSide.BUY,
                "XNAS", OrderType.LIMIT,
                "105.00", 10, 10, OrderStatus.NEW
        );

        Order sell = order(
                201L, 20L, "AAPL", OrderSide.SELL,
                "XNAS", OrderType.LIMIT,
                "100.00", 4, 4, OrderStatus.NEW
        );

        when(orderRepository
                .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                        eq("XNAS"), eq("AAPL"), eq(OrderSide.SELL), anyList(), eq(0L)
                )
        ).thenReturn(List.of(sell));

        TradeCreatedEvent tradeEvent = mock(TradeCreatedEvent.class);
        when(tradeEventFactory.created(any(), any(), any(), anyLong(), any()))
                .thenReturn(tradeEvent);

        // partial fill events
        when(orderEventFactory.partiallyFilled(any(Order.class), anyLong()))
                .thenReturn(mock(com.ab.orderservice.kafka.event.OrderPartiallyFilledEvent.class));
        when(orderEventFactory.filled(any(Order.class)))
                .thenReturn(mock(com.ab.orderservice.kafka.event.OrderFilledEvent.class));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        matchingService.match(buy);

        // BUY remaining 6, status PARTIALLY_FILLED
        assertThat(buy.getRemainingQuantity()).isEqualTo(6L);
        assertThat(buy.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);

        // SELL filled
        assertThat(sell.getRemainingQuantity()).isEqualTo(0L);
        assertThat(sell.getStatus()).isEqualTo(OrderStatus.FILLED);

        verify(tradeEventsProducer).publish(eq("101"), same(tradeEvent));

        // buy partially filled, sell filled
        verify(orderEventsProducer).publish(eq("101"), any());
        verify(orderEventsProducer).publish(eq("201"), any());
    }

    @Test
    void matchBuy_shouldBreak_whenBuyPriceTooLow_forBestSell() {
        Order buy = order(
                102L, 10L, "AAPL", OrderSide.BUY,
                "XNAS", OrderType.LIMIT,
                "90.00", 10, 10, OrderStatus.NEW
        );

        Order sell = order(
                202L, 20L, "AAPL", OrderSide.SELL,
                "XNAS", OrderType.LIMIT,
                "100.00", 10, 10, OrderStatus.NEW
        );

        when(orderRepository
                .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                        eq("XNAS"), eq("AAPL"), eq(OrderSide.SELL), anyList(), eq(0L)
                )
        ).thenReturn(List.of(sell));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        matchingService.match(buy);

        // No trade -> no publish
        verifyNoInteractions(tradeEventFactory);
        verify(tradeEventsProducer, never()).publish(anyString(), any());

        // incoming unchanged (still NEW)
        assertThat(buy.getRemainingQuantity()).isEqualTo(10L);
        assertThat(buy.getStatus()).isEqualTo(OrderStatus.NEW);

        // saved at end
        verify(orderRepository).save(buy);
        verify(orderRepository, never()).save(sell);
    }

    @Test
    void matchBuy_shouldSkipSelfTrade_andContinueToNextCandidate() {
        Order buy = order(
                103L, 10L, "AAPL", OrderSide.BUY,
                "XNAS", OrderType.LIMIT,
                "105.00", 10, 10, OrderStatus.NEW
        );

        Order sellSelf = order(
                203L, 10L, "AAPL", OrderSide.SELL,
                "XNAS", OrderType.LIMIT,
                "100.00", 10, 10, OrderStatus.NEW
        );

        Order sellOther = order(
                204L, 20L, "AAPL", OrderSide.SELL,
                "XNAS", OrderType.LIMIT,
                "100.00", 10, 10, OrderStatus.NEW
        );

        when(orderRepository
                .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                        eq("XNAS"), eq("AAPL"), eq(OrderSide.SELL), anyList(), eq(0L)
                )
        ).thenReturn(List.of(sellSelf, sellOther));

        TradeCreatedEvent tradeEvent = mock(TradeCreatedEvent.class);
        when(tradeEventFactory.created(any(), any(), any(), anyLong(), any()))
                .thenReturn(tradeEvent);

        when(orderEventFactory.filled(any(Order.class)))
                .thenReturn(mock(com.ab.orderservice.kafka.event.OrderFilledEvent.class));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        matchingService.match(buy);

        verify(tradeEventsProducer, times(1)).publish(eq("103"), same(tradeEvent));

        assertThat(buy.getRemainingQuantity()).isEqualTo(0L);
        assertThat(buy.getStatus()).isEqualTo(OrderStatus.FILLED);

        // self order untouched
        assertThat(sellSelf.getRemainingQuantity()).isEqualTo(10L);
        assertThat(sellSelf.getStatus()).isEqualTo(OrderStatus.NEW);

        // other filled
        assertThat(sellOther.getRemainingQuantity()).isEqualTo(0L);
        assertThat(sellOther.getStatus()).isEqualTo(OrderStatus.FILLED);
    }

    // SELL matching
    @Test
    void matchSell_shouldFillCompletely_againstOneBuy_andPublishTrade_andPersistBoth() {
        Order sell = order(
                300L, 20L, "AAPL", OrderSide.SELL,
                "XNAS", OrderType.LIMIT,
                "100.00", 5, 5, OrderStatus.NEW
        );

        Order buy = order(
                400L, 10L, "AAPL", OrderSide.BUY,
                "XNAS", OrderType.LIMIT,
                "105.00", 5, 5, OrderStatus.NEW
        );

        when(orderRepository
                .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
                        eq("XNAS"), eq("AAPL"), eq(OrderSide.BUY), anyList(), eq(0L)
                )
        ).thenReturn(List.of(buy));

        TradeCreatedEvent tradeEvent = mock(TradeCreatedEvent.class);
        when(tradeEventFactory.created(any(), any(), any(), anyLong(), any()))
                .thenReturn(tradeEvent);

        when(orderEventFactory.filled(any(Order.class)))
                .thenReturn(mock(com.ab.orderservice.kafka.event.OrderFilledEvent.class));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        matchingService.match(sell);

        assertThat(sell.getRemainingQuantity()).isEqualTo(0L);
        assertThat(sell.getStatus()).isEqualTo(OrderStatus.FILLED);

        assertThat(buy.getRemainingQuantity()).isEqualTo(0L);
        assertThat(buy.getStatus()).isEqualTo(OrderStatus.FILLED);

        // publish key is buy id
        verify(tradeEventsProducer).publish(eq("400"), same(tradeEvent));

        // filled events published for both orders
        verify(orderEventsProducer).publish(eq("400"), any());
        verify(orderEventsProducer).publish(eq("300"), any());

        verify(orderRepository, atLeastOnce()).save(buy);
        verify(orderRepository, atLeastOnce()).save(sell);
    }

    @Test
    void matchSell_shouldBreak_whenBestBuyPriceTooLow() {
        Order sell = order(
                301L, 20L, "AAPL", OrderSide.SELL,
                "XNAS", OrderType.LIMIT,
                "120.00", 5, 5, OrderStatus.NEW
        );

        Order buy = order(
                401L, 10L, "AAPL", OrderSide.BUY,
                "XNAS", OrderType.LIMIT,
                "110.00", 5, 5, OrderStatus.NEW
        );

        when(orderRepository
                .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
                        eq("XNAS"), eq("AAPL"), eq(OrderSide.BUY), anyList(), eq(0L)
                )
        ).thenReturn(List.of(buy));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        matchingService.match(sell);

        verifyNoInteractions(tradeEventFactory);
        verify(tradeEventsProducer, never()).publish(anyString(), any());

        assertThat(sell.getRemainingQuantity()).isEqualTo(5L);
        assertThat(sell.getStatus()).isEqualTo(OrderStatus.NEW);

        verify(orderRepository).save(sell);
        verify(orderRepository, never()).save(buy);
    }
}