package com.ab.orderservice.service;

import com.ab.orderservice.kafka.TradeEventFactory;
import com.ab.orderservice.kafka.TradeEventsProducer;
import com.ab.orderservice.kafka.event.TradeCreatedEvent;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
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

    @InjectMocks
    private MatchingService matchingService;

    // Helper to quickly build orders with defaults
    private Order order(Long id, Long userId, String instrument, OrderSide side,
                        String price, long qty, long remaining, OrderStatus status) {
        return Order.builder()
                .id(id)
                .userId(userId)
                .instrument(instrument)
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
        // service should do nothing if incoming has no remaining qty
        Order incoming = order(
                1L,
                10L,
                "AAPL",
                OrderSide.BUY,
                "100.00",
                10,
                0,
                OrderStatus.NEW);
        incoming.setRemainingQuantity(null);

        matchingService.match(incoming);

        verifyNoInteractions(orderRepository, tradeEventFactory, tradeEventsProducer);
    }

    @Test
    void match_shouldReturnImmediately_whenRemainingQuantityZeroOrNegative() {
        Order incoming = order(
                1L,
                10L,
                "AAPL",
                OrderSide.BUY,
                "100.00",
                10,
                0,
                OrderStatus.NEW);

        matchingService.match(incoming);

        verifyNoInteractions(orderRepository, tradeEventFactory, tradeEventsProducer);
    }

    @Test
    void match_shouldReturnImmediately_whenStatusNotActive() {
        // only NEW / PARTIALLY_FILLED are matched (ACTIVE_STATUSES)
        Order incoming = order(
                1L,
                10L,
                "AAPL",
                OrderSide.BUY,
                "100.00",
                10,
                10,
                OrderStatus.CANCELLED);

        matchingService.match(incoming);

        verifyNoInteractions(orderRepository, tradeEventFactory, tradeEventsProducer);
    }

    // BUY matching
    @Test
    void matchBuy_shouldFillCompletely_againstOneSell_andPublishEvent_andPersistBoth() {
        // incoming BUY: wants 10 at price 105
        Order buy = order(
                100L,
                10L,
                " AAPL ",
                OrderSide.BUY,
                "105.00",
                10,
                10,
                OrderStatus.NEW);

        // resting SELL: 10 at price 100 (matchable)
        Order sell = order(
                200L,
                20L,
                "AAPL",
                OrderSide.SELL,
                "100.00",
                10,
                10,
                OrderStatus.NEW);

        when(orderRepository
                .findByInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                        eq("AAPL"), eq(OrderSide.SELL), anyList(), eq(0L)))
                .thenReturn(List.of(sell));

        // event only that we publish it.
        TradeCreatedEvent event = mock(TradeCreatedEvent.class);
        when(tradeEventFactory.created(any(Order.class), any(Order.class), any(BigDecimal.class), anyLong(), any(LocalDateTime.class)))
                .thenReturn(event);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        matchingService.match(buy);

        // Assert - instrument trimmed + stored back into incoming
        assertThat(buy.getInstrument()).isEqualTo("AAPL");

        // After full fill, both become FILLED, remaining=0
        assertThat(buy.getRemainingQuantity()).isEqualTo(0L);
        assertThat(buy.getStatus()).isEqualTo(OrderStatus.FILLED);

        assertThat(sell.getRemainingQuantity()).isEqualTo(0L);
        assertThat(sell.getStatus()).isEqualTo(OrderStatus.FILLED);

        // Publish should use buy id as key (your code uses buy.getId())
        verify(tradeEventsProducer).publish(eq("100"), same(event));
        verify(orderRepository, atLeastOnce()).save(sell);
        verify(orderRepository, atLeastOnce()).save(buy);
    }

    @Test
    void matchBuy_shouldPartiallyFill_whenSellNotEnough() {
        // BUY wants 10
        Order buy = order(
                101L,
                10L,
                "AAPL",
                OrderSide.BUY,
                "105.00",
                10,
                10,
                OrderStatus.NEW);

        // SELL has only 4
        Order sell = order(
                201L,
                20L,
                "AAPL",
                OrderSide.SELL,
                "100.00",
                4,
                4,
                OrderStatus.NEW);

        when(orderRepository
                .findByInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                        eq("AAPL"), eq(OrderSide.SELL), anyList(), eq(0L)))
                .thenReturn(List.of(sell));

        TradeCreatedEvent event = mock(TradeCreatedEvent.class);
        when(tradeEventFactory.created(any(), any(), any(), anyLong(), any()))
                .thenReturn(event);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        matchingService.match(buy);

        // BUY remaining 6, status PARTIALLY_FILLED
        assertThat(buy.getRemainingQuantity()).isEqualTo(6L);
        assertThat(buy.getStatus()).isEqualTo(OrderStatus.PARTIALLY_FILLED);

        // SELL filled
        assertThat(sell.getRemainingQuantity()).isEqualTo(0L);
        assertThat(sell.getStatus()).isEqualTo(OrderStatus.FILLED);

        verify(tradeEventsProducer).publish(eq("101"), same(event));
    }

    @Test
    void matchBuy_shouldBreak_whenBuyPriceTooLow_forBestSell() {
        // BUY price 90
        Order buy = order(
                102L,
                10L,
                "AAPL",
                OrderSide.BUY,
                "90.00",
                10,
                10,
                OrderStatus.NEW);
        // Best SELL is 100 (not matchable), and because sells sorted ASC, we break immediately.
        Order sell = order(
                202L,
                20L,
                "AAPL",
                OrderSide.SELL,
                "100.00",
                10,
                10,
                OrderStatus.NEW);

        when(orderRepository
                .findByInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                        eq("AAPL"), eq(OrderSide.SELL), anyList(), eq(0L)))
                .thenReturn(List.of(sell));

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        matchingService.match(buy);

        // No trade happened -> no publish
        verifyNoInteractions(tradeEventFactory);
        verify(tradeEventsProducer, never()).publish(anyString(), any());

        // Status remains NEW, remaining unchanged
        assertThat(buy.getRemainingQuantity()).isEqualTo(10L);
        assertThat(buy.getStatus()).isEqualTo(OrderStatus.NEW);

        // Still saved at end
        verify(orderRepository).save(buy);
        // Resting order not saved because no match
        verify(orderRepository, never()).save(sell);
    }

    @Test
    void matchBuy_shouldSkipSelfTrade_andContinueToNextCandidate() {
        // BUY by user 10
        Order buy = order(
                103L,
                10L,
                "AAPL",
                OrderSide.BUY,
                "105.00",
                10,
                10,
                OrderStatus.NEW);

        // First SELL is same user -> should be skipped
        Order sellSelf = order(
                203L,
                10L,
                "AAPL",
                OrderSide.SELL,
                "100.00",
                10,
                10,
                OrderStatus.NEW);

        // Second SELL is different user -> should match
        Order sellOther = order(
                204L,
                20L,
                "AAPL",
                OrderSide.SELL,
                "100.00",
                10,
                10,
                OrderStatus.NEW);

        when(orderRepository
                .findByInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                        eq("AAPL"), eq(OrderSide.SELL), anyList(), eq(0L)))
                .thenReturn(List.of(sellSelf, sellOther));

        TradeCreatedEvent event = mock(TradeCreatedEvent.class);
        when(tradeEventFactory.created(any(), any(), any(), anyLong(), any()))
                .thenReturn(event);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        matchingService.match(buy);

        // Should trade exactly once (with sellOther)
        verify(tradeEventsProducer, times(1)).publish(eq("103"), same(event));

        // buy should be filled (10 matched)
        assertThat(buy.getRemainingQuantity()).isEqualTo(0L);
        assertThat(buy.getStatus()).isEqualTo(OrderStatus.FILLED);

        // sellSelf unchanged (skipped)
        assertThat(sellSelf.getRemainingQuantity()).isEqualTo(10L);
        assertThat(sellSelf.getStatus()).isEqualTo(OrderStatus.NEW);

        // sellOther filled
        assertThat(sellOther.getRemainingQuantity()).isEqualTo(0L);
        assertThat(sellOther.getStatus()).isEqualTo(OrderStatus.FILLED);
    }

    // SELL matching
    @Test
    void matchSell_shouldFillCompletely_againstOneBuy_andPublishEvent_andPersistBoth() {
        // incoming SELL: wants to sell 5 at 100
        Order sell = order(
                300L,
                20L,
                "AAPL",
                OrderSide.SELL,
                "100.00",
                5,
                5,
                OrderStatus.NEW);

        // resting BUY: price 105 (matchable), qty 5
        Order buy = order(
                400L,
                10L,
                "AAPL",
                OrderSide.BUY,
                "105.00",
                5,
                5,
                OrderStatus.NEW);

        when(orderRepository
                .findByInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
                        eq("AAPL"), eq(OrderSide.BUY), anyList(), eq(0L)))
                .thenReturn(List.of(buy));

        TradeCreatedEvent event = mock(TradeCreatedEvent.class);
        when(tradeEventFactory.created(any(), any(), any(), anyLong(), any()))
                .thenReturn(event);

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        matchingService.match(sell);

        // Trade price uses resting BUY price in matchSell
        assertThat(sell.getRemainingQuantity()).isEqualTo(0L);
        assertThat(sell.getStatus()).isEqualTo(OrderStatus.FILLED);

        assertThat(buy.getRemainingQuantity()).isEqualTo(0L);
        assertThat(buy.getStatus()).isEqualTo(OrderStatus.FILLED);

        // publish key is buy id
        verify(tradeEventsProducer).publish(eq("400"), same(event));

        verify(orderRepository, atLeastOnce()).save(buy);
        verify(orderRepository, atLeastOnce()).save(sell);
    }

    @Test
    void matchSell_shouldBreak_whenBestBuyPriceTooLow() {
        // SELL price 120, best BUY is 110 -> not matchable -> break
        Order sell = order(
                301L,
                20L,
                "AAPL",
                OrderSide.SELL,
                "120.00",
                5,
                5, OrderStatus.NEW);
        Order buy = order(
                401L,
                10L,
                "AAPL",
                OrderSide.BUY,
                "110.00",
                5,
                5,
                OrderStatus.NEW);

        when(orderRepository
                .findByInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
                        eq("AAPL"), eq(OrderSide.BUY), anyList(), eq(0L)))
                .thenReturn(List.of(buy));

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

