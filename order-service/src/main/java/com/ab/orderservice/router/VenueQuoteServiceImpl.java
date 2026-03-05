package com.ab.orderservice.router;

import com.ab.orderservice.dto.exchange.ExchangeInfo;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.model.enums.OrderType;
import com.ab.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VenueQuoteServiceImpl implements VenueQuoteService {
    private final OrderRepository orderRepository;
    private final VwapCalculator vwapCalculator;
    private final FeeCalculator feeCalculator;

    // Visible liquidity only (routing reads top-of-book + active book)
    private static final List<OrderStatus> VISIBLE_STATUSES =
            List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED);

    /**
     * Generates a comparable quote for one venue.
     * Usage: called once per exchange during route() / plan().
     */
    @Override
    public VenueQuote quote(String ex, ExchangeInfo info, SmartOrderRouter.RouteRequest req) {
        BigDecimal bestBid = bestBid(ex, req.instrument());
        BigDecimal bestAsk = bestAsk(ex, req.instrument());
        // Market orders always take; limit orders take when crossing the spread
        boolean isMarket = req.type() == OrderType.MARKET;
        boolean takerNow = isMarket || isCrossing(req.side(), req.limitPrice(), bestBid, bestAsk);

        // Computes fill + VWAP for taker path, or reference touch + fee-adjusted limit for maker path
        QuoteResult qr = takerNow
                ? takerQuote(ex, info, req)
                : makerQuote(info, req, bestBid, bestAsk);

        String reason = QuoteReasonBuilder.build(req.side(), takerNow, qr.estimatedFill(), bestBid, bestAsk, info, qr.vwap());

        return VenueQuote.builder()
                .exchangeCode(ex)
                .bestBid(bestBid)
                .bestAsk(bestAsk)
                .takerNow(takerNow)
                .estimatedFillQty(qr.estimatedFill())
                .estimatedExecPx(qr.vwap())
                .effectivePrice(qr.effectivePrice())
                .makerFeeBps(info.getMakerFeeBps())
                .takerFeeBps(info.getTakerFeeBps())
                .reason(reason)
                .build();
    }

    /**
     * Loads best bid for a venue.
     * Usage: sets touch price reference and crossing checks for SELL orders.
     */
    private BigDecimal bestBid(String ex, String instrument) {
        return orderRepository
                .findFirstByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
                        ex, instrument, OrderSide.BUY, VISIBLE_STATUSES, 0L
                )
                .map(Order::getPrice)
                .orElse(null);
    }

    /**
     * Loads best ask for a venue.
     * Usage: sets touch price reference and crossing checks for BUY orders.
     */
    private BigDecimal bestAsk(String ex, String instrument) {
        return orderRepository
                .findFirstByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                        ex, instrument, OrderSide.SELL, VISIBLE_STATUSES, 0L
                )
                .map(Order::getPrice)
                .orElse(null);
    }

    /**
     * Detects immediate execution possibility for limit orders.
     * Usage: switches between takerQuote() and makerQuote().
     */
    private boolean isCrossing(OrderSide side, BigDecimal limit, BigDecimal bestBid, BigDecimal bestAsk) {
        if (limit == null) return false;

        if (side == OrderSide.BUY) {
            return bestAsk != null && bestAsk.compareTo(limit) <= 0;
        }
        return bestBid != null && bestBid.compareTo(limit) >= 0;
    }

    /**
     * Taker simulation: walks the opposite book and computes VWAP + fee-adjusted price.
     * Usage: MARKET orders and crossing LIMIT orders.
     */
    private QuoteResult takerQuote(String ex, ExchangeInfo info, SmartOrderRouter.RouteRequest req) {
        boolean isMarket = req.type() == OrderType.MARKET;
        BigDecimal limit = isMarket ? null : req.limitPrice();

        if (req.side() == OrderSide.BUY) {
            List<Order> sells = orderRepository
                    .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                            ex, req.instrument(), OrderSide.SELL, VISIBLE_STATUSES, 0L
                    );

            VwapCalculator.VwapResult v = vwapCalculator.computeBuy(sells, req.qty(), limit);
            BigDecimal eff = feeCalculator.effectiveBuy(v.vwap(), info.getTakerFeeBps());

            return new QuoteResult(v.filled(), v.vwap(), eff);
        }

        List<Order> buys = orderRepository
                .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
                        ex, req.instrument(), OrderSide.BUY, VISIBLE_STATUSES, 0L
                );

        VwapCalculator.VwapResult v = vwapCalculator.computeSell(buys, req.qty(), limit);
        BigDecimal eff = feeCalculator.effectiveSell(v.vwap(), info.getTakerFeeBps());

        return new QuoteResult(v.filled(), v.vwap(), eff);
    }

    /**
     * Maker estimate: no immediate fill; keeps touch price as reference and applies maker fees to limit price.
     * Usage: non-crossing LIMIT orders.
     */
    private QuoteResult makerQuote(ExchangeInfo info, SmartOrderRouter.RouteRequest req, BigDecimal bestBid, BigDecimal bestAsk) {
        // EstimatedExecPx keeps touch for visibility; effectivePrice uses submitted limit + maker fee.
        if (req.side() == OrderSide.BUY) {
            BigDecimal vwap = bestAsk;
            BigDecimal eff = req.limitPrice() == null ? null : feeCalculator.effectiveBuy(req.limitPrice(), info.getMakerFeeBps());
            return new QuoteResult(0L, vwap, eff);
        }

        BigDecimal vwap = bestBid;
        BigDecimal eff = req.limitPrice() == null ? null : feeCalculator.effectiveSell(req.limitPrice(), info.getMakerFeeBps());
        return new QuoteResult(0L, vwap, eff);
    }

    /**
     * Internal transport object for quote calculations.
     * Usage: avoids passing multiple values through method chains.
     */
    private record QuoteResult(long estimatedFill, BigDecimal vwap, BigDecimal effectivePrice) {
    }
}
