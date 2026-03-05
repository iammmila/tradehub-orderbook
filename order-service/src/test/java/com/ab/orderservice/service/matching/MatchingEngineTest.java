package com.ab.orderservice.service.matching;

import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.model.enums.OrderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MatchingEngine:
 * - Applies matching rules + fills
 * - Delegates event publishing to MatchingEventPublisher
 */
@ExtendWith(MockitoExtension.class)
class MatchingEngineTest {

    @Mock
    private MatchingEventPublisher eventPublisher;

    private MatchingEngine engine;

    @BeforeEach
    void setUp() {// Real rule + fill logic, mocked publisher for side effects
        MatchRules rules = new MatchRules();
        OrderFillApplier fillApplier = new OrderFillApplier();
        // Fixed time for deterministic event timestamps
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-05T10:00:00Z"), ZoneOffset.UTC);

        engine = new MatchingEngine(rules, fillApplier, eventPublisher, fixedClock);
    }

    /**
     * Helper to build Order instances with minimal boilerplate.
     */
    private Order order(Long id, Long userId, String instrument, OrderSide side,
                        String exchangeCode, OrderType type,
                        String price, long qty, long remaining, OrderStatus status) {

        Order o = Order.builder()
                .id(id)
                .userId(userId)
                .instrument(instrument)
                .exchangeCode(exchangeCode)
                .type(type)
                .side(side)
                .price(price == null ? null : new BigDecimal(price))
                .quantity(qty)
                .remainingQuantity(remaining)
                .status(status)
                .build();
        o.setVisible(type != OrderType.HIDDEN_LIMIT);
        return o;
    }

    @Test
    void match_shouldFullFillBuyVsSell_publishTrade_andPublishOrderEvents() {
        Order buy = order(20L, 1L, "AAPL", OrderSide.BUY, "XNAS", OrderType.LIMIT, "150.0", 100, 100, OrderStatus.NEW);
        Order sell = order(10L, 2L, "AAPL", OrderSide.SELL, "XNAS", OrderType.LIMIT, "150.0", 100, 100, OrderStatus.NEW);

        engine.match(buy, List.of(sell));

        assertThat(buy.getRemainingQuantity()).isEqualTo(0L);
        assertThat(buy.getStatus()).isEqualTo(OrderStatus.FILLED);

        assertThat(sell.getRemainingQuantity()).isEqualTo(0L);
        assertThat(sell.getStatus()).isEqualTo(OrderStatus.FILLED);

        // tradePrice = resting price (sell price here)
        verify(eventPublisher).publishTradeCreated(
                eq(buy),
                eq(sell),
                eq(new BigDecimal("150.0")),
                eq(100L),
                any(LocalDateTime.class)
        );

        // engine publishes fill events for both
        verify(eventPublisher).publishOrderFillIfNeeded(eq(buy), eq(100L));
        verify(eventPublisher).publishOrderFillIfNeeded(eq(sell), eq(100L));
    }

    @Test
    void match_shouldSkipSelfTrade_andContinue() {
        Order buy = order(20L, 1L, "AAPL", OrderSide.BUY, "XNAS", OrderType.LIMIT, "150.0", 100, 100, OrderStatus.NEW);
        Order sellSelf = order(10L, 1L, "AAPL", OrderSide.SELL, "XNAS", OrderType.LIMIT, "150.0", 100, 100, OrderStatus.NEW);
        Order sellOther = order(11L, 2L, "AAPL", OrderSide.SELL, "XNAS", OrderType.LIMIT, "150.0", 100, 100, OrderStatus.NEW);

        engine.match(buy, List.of(sellSelf, sellOther));

        // only sellOther matched
        assertThat(buy.getStatus()).isEqualTo(OrderStatus.FILLED);
        assertThat(sellSelf.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(sellOther.getStatus()).isEqualTo(OrderStatus.FILLED);

        verify(eventPublisher, times(1)).publishTradeCreated(eq(buy), eq(sellOther), any(), eq(100L), any());
        verify(eventPublisher, never()).publishTradeCreated(eq(buy), eq(sellSelf), any(), anyLong(), any());
    }

    @Test
    void match_shouldNotMatch_whenBuyPriceTooLow() {
        Order buy = order(20L, 1L, "AAPL", OrderSide.BUY, "XNAS", OrderType.LIMIT, "90.0", 100, 100, OrderStatus.NEW);
        Order sell = order(10L, 2L, "AAPL", OrderSide.SELL, "XNAS", OrderType.LIMIT, "150.0", 100, 100, OrderStatus.NEW);

        engine.match(buy, List.of(sell));

        assertThat(buy.getRemainingQuantity()).isEqualTo(100L);
        assertThat(buy.getStatus()).isEqualTo(OrderStatus.NEW);

        assertThat(sell.getRemainingQuantity()).isEqualTo(100L);
        assertThat(sell.getStatus()).isEqualTo(OrderStatus.NEW);

        verifyNoInteractions(eventPublisher);
    }
}
