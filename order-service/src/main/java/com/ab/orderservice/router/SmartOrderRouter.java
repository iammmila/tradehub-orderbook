package com.ab.orderservice.router;

import com.ab.orderservice.dto.exchange.ExchangeInfo;
import com.ab.orderservice.service.ExchangeRegistry;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.model.enums.OrderType;
import com.ab.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SmartOrderRouter {
    private final OrderRepository orderRepository;
    private final ExchangeRegistry exchangeRegistry;

    private static final List<OrderStatus> VISIBLE_STATUSES =
            List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED);

    public RouteDecision route(String instrument, OrderSide side, OrderType type, BigDecimal limitPrice, long qty) {
        String inst = instrument.trim().toUpperCase(Locale.ROOT);

        List<VenueQuote> quotes = new ArrayList<>();

        for (String ex : exchangeRegistry.codes()) {
            ExchangeInfo info = exchangeRegistry.info(ex);

            VenueQuote q = quoteVenue(ex, info, inst, side, type, limitPrice, qty);
            quotes.add(q);
        }

        // Choose best venue
        String chosen = chooseBest(quotes, side);
        if (chosen == null || chosen.isBlank() || !exchangeRegistry.isSupported(chosen)) {
            chosen = exchangeRegistry.normalizeOrDefault(null);
        }

        String finalChosen = chosen;
        String reason = quotes.stream()
                .filter(v -> v.getExchangeCode().equals(finalChosen))
                .map(VenueQuote::getReason)
                .findFirst()
                .orElse("DEFAULT");

        return RouteDecision.builder()
                .chosenExchange(chosen)
                .reason(reason)
                .quotes(quotes)
                .build();
    }

    private VenueQuote quoteVenue(
            String ex,
            ExchangeInfo info,
            String instrument,
            OrderSide side,
            OrderType type,
            BigDecimal limitPrice,
            long qty
    ) {
        // Top of book
        BigDecimal bestBid = orderRepository
                .findFirstByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
                        ex, instrument, OrderSide.BUY, VISIBLE_STATUSES, 0L
                )
                .map(Order::getPrice)
                .orElse(null);

        BigDecimal bestAsk = orderRepository
                .findFirstByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                        ex, instrument, OrderSide.SELL, VISIBLE_STATUSES, 0L
                )
                .map(Order::getPrice)
                .orElse(null);

        boolean isMarket = type == OrderType.MARKET;

        boolean takerNow;
        if (isMarket) {
            takerNow = true; // MARKET always takes liquidity (if any exists)
        } else {
            if (side == OrderSide.BUY) {
                takerNow = bestAsk != null && limitPrice != null && bestAsk.compareTo(limitPrice) <= 0;
            } else {
                takerNow = bestBid != null && limitPrice != null && bestBid.compareTo(limitPrice) >= 0;
            }
        }

        long estimatedFill = 0L;
        BigDecimal estimatedExecPx = null;
        BigDecimal effectivePrice = null;

        if (takerNow) {
            // Estimate liquidity available within limit (or all liquidity for MARKET)
            if (side == OrderSide.BUY) {
                var sells = orderRepository
                        .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                                ex, instrument, OrderSide.SELL, VISIBLE_STATUSES, 0L
                        );

                estimatedFill = estimateFillBuy(sells, qty, isMarket ? null : limitPrice);
                estimatedExecPx = bestAsk; // simple proxy (not full VWAP)
                effectivePrice = effectiveBuyPrice(estimatedExecPx, info.getTakerFeeBps());
            } else {
                var buys = orderRepository
                        .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
                                ex, instrument, OrderSide.BUY, VISIBLE_STATUSES, 0L
                        );

                estimatedFill = estimateFillSell(buys, qty, isMarket ? null : limitPrice);
                estimatedExecPx = bestBid;
                effectivePrice = effectiveSellPrice(estimatedExecPx, info.getTakerFeeBps());
            }
        } else {
            if (side == OrderSide.BUY) {
                estimatedExecPx = bestAsk;
                effectivePrice = (limitPrice != null)
                        ? effectiveBuyPrice(limitPrice, info.getMakerFeeBps())
                        : null;
            } else {
                estimatedExecPx = bestBid;
                effectivePrice = (limitPrice != null)
                        ? effectiveSellPrice(limitPrice, info.getMakerFeeBps())
                        : null;
            }
        }

        String reason = buildReason(side, takerNow, estimatedFill, bestBid, bestAsk, info);

        return VenueQuote.builder()
                .exchangeCode(ex)
                .bestBid(bestBid)
                .bestAsk(bestAsk)
                .takerNow(takerNow)
                .estimatedFillQty(estimatedFill)
                .estimatedExecPx(estimatedExecPx)
                .effectivePrice(effectivePrice)
                .makerFeeBps(info.getMakerFeeBps())
                .takerFeeBps(info.getTakerFeeBps())
                .reason(reason)
                .build();
    }

    private static long estimateFillBuy(List<Order> sells, long desiredQty, BigDecimal limitPriceOrNull) {
        long remaining = desiredQty;
        for (Order s : sells) {
            if (remaining <= 0) break;
            if (limitPriceOrNull != null && s.getPrice().compareTo(limitPriceOrNull) > 0) break;
            long take = Math.min(remaining, s.getRemainingQuantity());
            remaining -= take;
        }
        return desiredQty - remaining;
    }

    private static long estimateFillSell(List<Order> buys, long desiredQty, BigDecimal limitPriceOrNull) {
        long remaining = desiredQty;
        for (Order b : buys) {
            if (remaining <= 0) break;
            if (limitPriceOrNull != null && b.getPrice().compareTo(limitPriceOrNull) < 0) break;
            long take = Math.min(remaining, b.getRemainingQuantity());
            remaining -= take;
        }
        return desiredQty - remaining;
    }

    private static BigDecimal effectiveBuyPrice(BigDecimal px, int feeBps) {
        if (px == null) return null;
        // BUY pays price + fees
        BigDecimal fee = BigDecimal.valueOf(feeBps).divide(BigDecimal.valueOf(10_000), 8, RoundingMode.HALF_UP);
        return px.multiply(BigDecimal.ONE.add(fee));
    }

    private static BigDecimal effectiveSellPrice(BigDecimal px, int feeBps) {
        if (px == null) return null;
        BigDecimal fee = BigDecimal.valueOf(feeBps).divide(BigDecimal.valueOf(10_000), 8, RoundingMode.HALF_UP);
        return px.multiply(BigDecimal.ONE.subtract(fee));
    }

    private static String chooseBest(List<VenueQuote> quotes, OrderSide side) {
        Comparator<VenueQuote> cmp = (a, b) -> {
            int fill = Long.compare(b.getEstimatedFillQty(), a.getEstimatedFillQty()); // bigger fill first
            if (fill != 0) return fill;

            BigDecimal ea = a.getEffectivePrice();
            BigDecimal eb = b.getEffectivePrice();

            if (ea == null && eb == null) return a.getExchangeCode().compareTo(b.getExchangeCode());
            if (ea == null) return 1;
            if (eb == null) return -1;

            int priceCmp;
            if (side == OrderSide.BUY) {
                priceCmp = ea.compareTo(eb); // lower is better
            } else {
                priceCmp = eb.compareTo(ea); // higher is better
            }
            if (priceCmp != 0) return priceCmp;

            return a.getExchangeCode().compareTo(b.getExchangeCode());
        };

        return quotes
                .stream()
                .min(cmp)
                .map(VenueQuote::getExchangeCode)
                .orElse("XLON");
    }

    private static String buildReason(OrderSide side, boolean takerNow, long fill, BigDecimal bestBid, BigDecimal bestAsk, ExchangeInfo info) {
        if (takerNow) {
            return "TAKER_NOW: fill=" + fill + " takerFeeBps=" + info.getTakerFeeBps()
                    + " top=" + (side == OrderSide.BUY ? bestAsk : bestBid);
        }
        return "MAKER: makerFeeBps=" + info.getMakerFeeBps()
                + " touch=" + (side == OrderSide.BUY ? bestAsk : bestBid);
    }
}
