package com.ab.orderservice.service;

import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.model.enums.OrderType;
import com.ab.orderservice.repository.OrderRepository;
import com.ab.orderservice.service.matching.MatchCandidateFinder;
import com.ab.orderservice.service.matching.MatchingEngine;
import com.ab.orderservice.service.matching.OrderNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MatchingService orchestration:
 * - Checks matchable gate + normalization
 * - Finds candidates + delegates to engine
 * - Persists touched resting orders + incoming order
 */
@ExtendWith(MockitoExtension.class)
public class MatchingServiceTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private MatchCandidateFinder candidateFinder;
    @Mock
    private MatchingEngine engine;

    private OrderNormalizer normalizer;
    private MatchingService matchingService;

    @BeforeEach
    void setUp() {  // Real normalizer for trimming + exchangeCode validation
        normalizer = new OrderNormalizer();
        matchingService = new MatchingService(orderRepository, normalizer, candidateFinder, engine);
    }

    // Helper to quickly build orders with defaults
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
                .createdAt(LocalDateTime.now())
                .build();
        o.setVisible(type != OrderType.HIDDEN_LIMIT);
        return o;
    }

    @Test
    void match_shouldReturnImmediately_whenIncomingNotMatchable_nullRemaining() {
        Order incoming = order(1L, 10L, "AAPL", OrderSide.BUY, "XNAS", OrderType.LIMIT, "100.00", 10, 10, OrderStatus.NEW);
        incoming.setRemainingQuantity(null);

        matchingService.match(incoming);
        // No work when incoming is not matchable
        verifyNoInteractions(candidateFinder, engine, orderRepository);
    }

    @Test
    void match_shouldReturnImmediately_whenIncomingNotMatchable_zeroRemaining() {
        Order incoming = order(1L, 10L, "AAPL", OrderSide.BUY, "XNAS", OrderType.LIMIT, "100.00", 10, 0, OrderStatus.NEW);

        matchingService.match(incoming);

        verifyNoInteractions(candidateFinder, engine, orderRepository);
    }

    @Test
    void match_shouldThrow_whenExchangeCodeMissing() {
        Order incoming = order(1L, 10L, "AAPL", OrderSide.BUY, null, OrderType.LIMIT, "100.00", 10, 10, OrderStatus.NEW);

        assertThatThrownBy(() -> matchingService.match(incoming))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exchangeCode");
        // Fails before candidate lookup and persistence
        verifyNoInteractions(candidateFinder, engine);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void match_shouldNormalize_findCandidates_engineMatch_andPersistTouchedAndIncoming() {
        Order incoming = order(100L, 10L, " aapl ", OrderSide.BUY, "xnas", OrderType.LIMIT, "105.00", 10, 10, OrderStatus.NEW);

        Order touched1 = order(200L, 20L, "AAPL", OrderSide.SELL, "XNAS", OrderType.LIMIT, "100.00", 10, 0, OrderStatus.FILLED);
        Order touched2 = order(201L, 21L, "AAPL", OrderSide.SELL, "XNAS", OrderType.LIMIT, "101.00", 10, 5, OrderStatus.PARTIALLY_FILLED);

        when(candidateFinder.findCandidates(any(Order.class))).thenReturn(List.of(touched1, touched2));
        when(engine.match(eq(incoming), anyList())).thenReturn(Set.of(touched1, touched2));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        matchingService.match(incoming);

        // normalized by real normalizer
        org.assertj.core.api.Assertions.assertThat(incoming.getInstrument()).isEqualTo("AAPL");
        org.assertj.core.api.Assertions.assertThat(incoming.getExchangeCode()).isEqualTo("XNAS");

        verify(candidateFinder).findCandidates(incoming);
        verify(engine).match(eq(incoming), anyList());

        // saves each touched order + incoming
        verify(orderRepository).save(touched1);
        verify(orderRepository).save(touched2);
        verify(orderRepository).save(incoming);
    }
}