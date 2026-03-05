package com.ab.orderservice.service;

import com.ab.orderservice.dto.OrderResponse;
import com.ab.orderservice.dto.orderbook.OrderBookLevel;
import com.ab.orderservice.dto.orderbook.OrderBookResponse;
import com.ab.orderservice.exception.BadRequestException;
import com.ab.orderservice.exception.enums.ErrorCode;
import com.ab.orderservice.mapper.OrderMapper;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Read-only order book queries (per exchange or aggregated).
 * Supports optional price levels for UI depth charts.
 */
@Service
@RequiredArgsConstructor
public class OrderBookService {
    private final OrderRepository orderRepository;
    private final ExchangeRegistry exchangeRegistry;

    // Statuses that appear in the public order book.
    private static final List<OrderStatus> VISIBLE_STATUSES =
            List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED);

    /**
     * Returns order book for an instrument (per exchange or aggregated).
     * levels=true adds bidLevels/askLevels aggregated by price.
     */
    public OrderBookResponse getOrderBook(String instrument, String exchange, boolean aggregated, boolean levels) {
        String inst = normalizeInstrument(instrument);
        validateExchangeIfProvided(exchange);

        if (aggregated) {
            OrderBookResponse book = buildAggregatedBook(inst);
            if (levels) applyLevels(book);
            book.setLevels(levels);
            return book;
        }
        String ex = exchangeRegistry.normalizeOrDefault(exchange);
        OrderBookResponse book = buildPerExchangeBook(inst, ex);
        book.setLevels(false);
        return book;
    }

    // Convenience endpoint: returns only bid or ask levels for one exchange.
    public List<OrderBookLevel> getLevelsForExchange(String instrument, String exchange, boolean isBid) {
        String inst = normalizeInstrument(instrument);
        String ex = exchangeRegistry.normalizeOrDefault(exchange);

        OrderBookResponse book = buildPerExchangeBook(inst, ex);
        return isBid
                ? toLevels(book.getBids(), true)
                : toLevels(book.getAsks(), false);
    }

    // Canonical instrument form used for repository filtering.
    private String normalizeInstrument(String instrument) {
        if (instrument == null || instrument.isBlank()) {
            throw new BadRequestException(ErrorCode.INSTRUMENT_REQUIRED);
        }
        return instrument.trim().toUpperCase(Locale.ROOT);
    }

    // Checks exchange only when user passed a non-blank value.
    private void validateExchangeIfProvided(String exchange) {
        if (exchange == null || exchange.isBlank()) return;
        if (!exchangeRegistry.isSupported(exchange)) {
            throw new BadRequestException(ErrorCode.EXCHANGE_NOT_SUPPORTED);
        }
    }

    // Builds book for one exchange.
    private OrderBookResponse buildPerExchangeBook(String instrument, String exchangeCode) {
        List<Order> bidOrders = findVisibleOrders(exchangeCode, instrument, OrderSide.BUY);
        List<Order> askOrders = findVisibleOrders(exchangeCode, instrument, OrderSide.SELL);

        return OrderBookResponse.builder()
                .instrument(instrument)
                .exchange(exchangeCode)
                .aggregated(false)
                .bids(mapToResponses(bidOrders))
                .asks(mapToResponses(askOrders))
                .build();
    }

    // Builds merged book across all supported exchanges.
    private OrderBookResponse buildAggregatedBook(String instrument) {
        List<Order> allBids = new ArrayList<>();
        List<Order> allAsks = new ArrayList<>();

        for (String ex : exchangeRegistry.codes()) {
            allBids.addAll(findVisibleOrders(ex, instrument, OrderSide.BUY));
            allAsks.addAll(findVisibleOrders(ex, instrument, OrderSide.SELL));
        }

        sortBookSide(allBids, true);
        sortBookSide(allAsks, false);

        return OrderBookResponse.builder()
                .instrument(instrument)
                .exchange(null)
                .aggregated(true)
                .bids(mapToResponses(allBids))
                .asks(mapToResponses(allAsks))
                .build();
    }

    // Repository access isolated to one method for easier changes later.
    private List<Order> findVisibleOrders(String exchangeCode, String instrument, OrderSide side) {
        if (side == OrderSide.BUY) {
            return orderRepository
                    .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
                            exchangeCode, instrument, OrderSide.BUY, VISIBLE_STATUSES, 0L
                    );
        }

        return orderRepository
                .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                        exchangeCode, instrument, OrderSide.SELL, VISIBLE_STATUSES, 0L
                );
    }

    // Domain sorting rules for book display.
    private void sortBookSide(List<Order> orders, boolean isBid) {
        Comparator<Order> cmp = Comparator.comparing(Order::getPrice)
                .thenComparing(Order::getCreatedAt);

        // Bids: highest price first; asks: lowest price first.
        if (isBid) cmp = cmp.reversed();

        orders.sort(cmp);
    }

    // DTO mapping kept in one place.
    private List<OrderResponse> mapToResponses(List<Order> orders) {
        return orders.stream()
                .map(OrderMapper::toResponse)
                .toList();
    }

    // Adds price levels to an existing book response.
    private void applyLevels(OrderBookResponse book) {
        book.setBidLevels(toLevels(book.getBids(), true));
        book.setAskLevels(toLevels(book.getAsks(), false));
    }

    // Groups orders by normalized price and sums remainingQuantity.
    private List<OrderBookLevel> toLevels(List<OrderResponse> orders, boolean isBid) {
        if (orders == null || orders.isEmpty()) return List.of();

        Map<BigDecimal, Long> quantityByPrice = new HashMap<>();

        for (OrderResponse o : orders) {
            if (o == null || o.getPrice() == null) continue;

            long rem = (o.getRemainingQuantity() == null) ? 0L : o.getRemainingQuantity();
            if (rem <= 0) continue;

            BigDecimal priceKey = normalizePrice(o.getPrice());
            quantityByPrice.merge(priceKey, rem, Long::sum);
        }

        Comparator<OrderBookLevel> cmp = Comparator.comparing(OrderBookLevel::getPrice);
        if (isBid) cmp = cmp.reversed();

        return quantityByPrice.entrySet().stream()
                .map(e -> OrderBookLevel.builder()
                        .price(e.getKey())
                        .totalQuantity(e.getValue())
                        .build())
                .sorted(cmp)
                .collect(Collectors.toList());
    }

    // Price normalization used for stable grouping keys.
    private BigDecimal normalizePrice(BigDecimal price) {
        return price.setScale(4, RoundingMode.HALF_UP);
    }
}