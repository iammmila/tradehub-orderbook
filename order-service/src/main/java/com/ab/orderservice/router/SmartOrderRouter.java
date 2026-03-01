package com.ab.orderservice.router;

import com.ab.orderservice.dto.exchange.ExchangeInfo;
import com.ab.orderservice.dto.route.RouteEstimateDto;
import com.ab.orderservice.dto.route.RoutePlanResponse;
import com.ab.orderservice.service.ExchangeRegistry;
import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.model.enums.OrderType;
import com.ab.orderservice.repository.OrderRepository;
import com.ab.orderservice.service.OrderBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
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
                .filter(v -> Objects.equals(v.getExchangeCode(), finalChosen))
                .map(VenueQuote::getReason)
                .findFirst()
                .orElse("DEFAULT");

        logAutoRouting(inst, side, qty, finalChosen, reason, quotes);

        return RouteDecision.builder()
                .chosenExchange(finalChosen)
                .reason(reason)
                .quotes(quotes)
                .build();
    }

    public RoutePlanResponse plan(String instrument, OrderSide side, OrderType type, BigDecimal limitPrice, long qty) {
        String inst = instrument.trim().toUpperCase(Locale.ROOT);

        List<VenueQuote> quotes = new ArrayList<>();

        for (String ex : exchangeRegistry.codes()) {
            ExchangeInfo info = exchangeRegistry.info(ex);
            VenueQuote q = quoteVenue(ex, info, inst, side, type, limitPrice, qty);
            quotes.add(q);
        }

        // same selection logic
        String chosen = chooseBest(quotes, side);
        if (chosen == null || chosen.isBlank() || !exchangeRegistry.isSupported(chosen)) {
            chosen = exchangeRegistry.normalizeOrDefault(null);
        }

        // convert VenueQuote -> RouteEstimateDto and sort in the same ranking order
        List<RouteEstimateDto> ranked = quotes.stream()
                .map(v -> RouteEstimateDto.builder()
                        .exchange(v.getExchangeCode())
                        .fillQuantity(v.getEstimatedFillQty())
                        .vwap(v.getEstimatedExecPx())          // your VWAP is in estimatedExecPx now
                        .effectiveVwap(v.getEffectivePrice())  // effective VWAP
                        .reason(v.getReason())
                        .build())
                .sorted((a, b) -> {
                    int fill = Long.compare(
                            b.getFillQuantity() == null ? 0 : b.getFillQuantity(),
                            a.getFillQuantity() == null ? 0 : a.getFillQuantity()
                    );
                    if (fill != 0) return fill;

                    BigDecimal ea = a.getEffectiveVwap();
                    BigDecimal eb = b.getEffectiveVwap();

                    if (ea == null && eb == null) return a.getExchange().compareTo(b.getExchange());
                    if (ea == null) return 1;
                    if (eb == null) return -1;

                    int priceCmp = (side == OrderSide.BUY) ? ea.compareTo(eb) : eb.compareTo(ea);
                    if (priceCmp != 0) return priceCmp;

                    return a.getExchange().compareTo(b.getExchange());
                })
                .toList();

        // log it (top3)
        String finalChosen = chosen;
        String reason = ranked.stream()
                .filter(r -> Objects.equals(r.getExchange(), finalChosen))
                .map(RouteEstimateDto::getReason)
                .findFirst()
                .orElse("DEFAULT");

        log.info("ROUTE_PLAN inst={} side={} qty={} chosen={} reason={} top3={}",
                inst, side, qty, chosen, reason, top3String(ranked));

        return RoutePlanResponse.builder()
                .chosenExchange(chosen)
                .ranked(ranked)
                .build();
    }

    private String top3String(List<RouteEstimateDto> ranked) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(3, ranked.size()); i++) {
            var r = ranked.get(i);
            sb.append(i + 1).append(") ")
                    .append(r.getExchange())
                    .append(" fill=").append(r.getFillQuantity())
                    .append(" eff=").append(r.getEffectiveVwap())
                    .append(" vwap=").append(r.getVwap())
                    .append(" | ");
        }
        return sb.toString();
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
            if (side == OrderSide.BUY) {
                var sells = orderRepository
                        .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                                ex, instrument, OrderSide.SELL, VISIBLE_STATUSES, 0L
                        );

                estimatedFill = estimateFillBuy(sells, qty, isMarket ? null : limitPrice);
                VwapResult v = computeBuyVwap(sells, qty, isMarket ? null : limitPrice);
                estimatedExecPx = v.vwap; // VWAP
                effectivePrice = effectiveBuyPrice(estimatedExecPx, info.getTakerFeeBps());
            } else {
                var buys = orderRepository
                        .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
                                ex, instrument, OrderSide.BUY, VISIBLE_STATUSES, 0L
                        );

                estimatedFill = estimateFillSell(buys, qty, isMarket ? null : limitPrice);
                VwapResult v = computeSellVwap(buys, qty, isMarket ? null : limitPrice);
                estimatedExecPx = v.vwap; // VWAP
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

        String reason = buildReason(side, takerNow, estimatedFill, bestBid, bestAsk, info, estimatedExecPx);

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

    private static VwapResult computeBuyVwap(List<Order> sellsAsc, long desiredQty, BigDecimal limitPriceOrNull) {
        long remaining = desiredQty;
        BigDecimal notional = BigDecimal.ZERO;
        long filled = 0;

        for (Order s : sellsAsc) {
            if (remaining <= 0) break;
            if (limitPriceOrNull != null && s.getPrice().compareTo(limitPriceOrNull) > 0) break;

            long take = Math.min(remaining, s.getRemainingQuantity());
            if (take <= 0) continue;

            filled += take;
            remaining -= take;
            notional = notional.add(s.getPrice().multiply(BigDecimal.valueOf(take)));
        }

        if (filled == 0) return new VwapResult(0, null);

        BigDecimal vwap = notional.divide(BigDecimal.valueOf(filled), 8, RoundingMode.HALF_UP);
        return new VwapResult(filled, vwap);
    }

    private static VwapResult computeSellVwap(List<Order> buysDesc, long desiredQty, BigDecimal limitPriceOrNull) {
        long remaining = desiredQty;
        BigDecimal notional = BigDecimal.ZERO;
        long filled = 0;

        for (Order b : buysDesc) {
            if (remaining <= 0) break;
            if (limitPriceOrNull != null && b.getPrice().compareTo(limitPriceOrNull) < 0) break;

            long take = Math.min(remaining, b.getRemainingQuantity());
            if (take <= 0) continue;

            filled += take;
            remaining -= take;
            notional = notional.add(b.getPrice().multiply(BigDecimal.valueOf(take)));
        }

        if (filled == 0) return new VwapResult(0, null);

        BigDecimal vwap = notional.divide(BigDecimal.valueOf(filled), 8, RoundingMode.HALF_UP);
        return new VwapResult(filled, vwap);
    }

    private record VwapResult(long filled, BigDecimal vwap) {
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

    private static String buildReason(
            OrderSide side,
            boolean takerNow,
            long fill,
            BigDecimal bestBid,
            BigDecimal bestAsk,
            ExchangeInfo info,
            BigDecimal vwapOrNull
    ) {
        if (takerNow) {
            return "TAKER_VWAP: fill=" + fill
                    + " takerFeeBps=" + info.getTakerFeeBps()
                    + " vwap=" + vwapOrNull
                    + " touch=" + (side == OrderSide.BUY ? bestAsk : bestBid);
        }
        return "MAKER: makerFeeBps=" + info.getMakerFeeBps()
                + " touch=" + (side == OrderSide.BUY ? bestAsk : bestBid);
    }

    private void logAutoRouting(String inst, OrderSide side, long qty, String chosen, String reason, List<VenueQuote> quotes) {
        // Sort same as chooseBest ranking, then take top3 for log
        List<VenueQuote> sorted = new ArrayList<>(quotes);
        String best = chooseBest(sorted, side);
        // If best is computed by chooseBest, we still want top3 ordering:
        // We'll reuse comparator by sorting with same logic:
        sorted.sort((a, b) -> {
            int fill = Long.compare(b.getEstimatedFillQty(), a.getEstimatedFillQty());
            if (fill != 0) return fill;

            BigDecimal ea = a.getEffectivePrice();
            BigDecimal eb = b.getEffectivePrice();

            if (ea == null && eb == null) return a.getExchangeCode().compareTo(b.getExchangeCode());
            if (ea == null) return 1;
            if (eb == null) return -1;

            int priceCmp = (side == OrderSide.BUY) ? ea.compareTo(eb) : eb.compareTo(ea);
            if (priceCmp != 0) return priceCmp;

            return a.getExchangeCode().compareTo(b.getExchangeCode());
        });

        StringBuilder top3 = new StringBuilder();
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            VenueQuote q = sorted.get(i);
            top3.append(i + 1).append(") ")
                    .append(q.getExchangeCode())
                    .append(" fill=").append(q.getEstimatedFillQty())
                    .append(" eff=").append(q.getEffectivePrice())
                    .append(" vwap=").append(q.getEstimatedExecPx())
                    .append(" | ");
        }

        log.info("AUTO_ROUTE inst={} side={} qty={} chosen={} reason={} top3={}",
                inst, side, qty, chosen, reason, top3);
    }
}
