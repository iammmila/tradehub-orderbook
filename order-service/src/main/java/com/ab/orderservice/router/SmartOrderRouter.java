package com.ab.orderservice.router;

import com.ab.orderservice.dto.exchange.ExchangeInfo;
import com.ab.orderservice.dto.route.RouteEstimateDto;
import com.ab.orderservice.dto.route.RoutePlanResponse;
import com.ab.orderservice.service.ExchangeRegistry;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartOrderRouter {
    private final ExchangeRegistry exchangeRegistry;
    private final VenueQuoteService quoteService;
    private final VenueRankingService rankingService;
    private final RoutingLogFormatter logFormatter;

    /**
     * Picks one exchange for an incoming order based on estimated fill + effective price.
     * Usage: call during order submission when exchangeCode is AUTO or missing.
     */
    public RouteDecision route(String instrument, OrderSide side, OrderType type, BigDecimal limitPrice, long qty) {
        RouteRequest req = RouteRequest.of(instrument, side, type, limitPrice, qty);
        // Builds one quote per supported exchange
        List<VenueQuote> quotes = buildQuotes(req);

        String chosen = pickOrDefault(quotes, side);
        String reason = quotes.stream()// Stores human-readable explanation from the chosen venue quote
                .filter(q -> Objects.equals(q.getExchangeCode(), chosen))
                .map(VenueQuote::getReason)
                .findFirst()
                .orElse("DEFAULT");

        log.info("AUTO_ROUTE {}", logFormatter.autoRoute(req, chosen, reason, quotes, side));

        return RouteDecision.builder()
                .chosenExchange(chosen)
                .reason(reason)
                .quotes(quotes)
                .build();
    }

    /**
     * Returns ranked venues with estimates for UI / diagnostics.
     * Usage: call from "route plan" endpoint to show ranking and selected exchange.
     */
    public RoutePlanResponse plan(String instrument, OrderSide side, OrderType type, BigDecimal limitPrice, long qty) {
        RouteRequest req = RouteRequest.of(instrument, side, type, limitPrice, qty);

        List<VenueQuote> quotes = buildQuotes(req);
        List<VenueQuote> rankedQuotes = rankingService.rank(quotes, side);

        String chosen = pickOrDefault(rankedQuotes, side);

        List<RouteEstimateDto> ranked = rankedQuotes.stream()
                .map(v -> RouteEstimateDto.builder()
                        .exchange(v.getExchangeCode())
                        .fillQuantity(v.getEstimatedFillQty())
                        .vwap(v.getEstimatedExecPx())
                        .effectiveVwap(v.getEffectivePrice())
                        .reason(v.getReason())
                        .build())
                .toList();

        String reason = ranked.stream()
                .filter(r -> Objects.equals(r.getExchange(), chosen))
                .map(RouteEstimateDto::getReason)
                .findFirst()
                .orElse("DEFAULT");

        log.info("ROUTE_PLAN {}", logFormatter.routePlan(req, chosen, reason, ranked));

        return RoutePlanResponse.builder()
                .chosenExchange(chosen)
                .ranked(ranked)
                .build();
    }

    /**
     * Builds venue quotes for all supported exchanges.
     * Usage: shared helper for both route() and plan().
     */
    private List<VenueQuote> buildQuotes(RouteRequest req) {
        List<VenueQuote> quotes = new ArrayList<>();
        for (String ex : exchangeRegistry.codes()) {
            ExchangeInfo info = exchangeRegistry.info(ex);
            if (info == null) continue;
            quotes.add(quoteService.quote(ex, info, req));
        }
        return quotes;
    }

    /**
     * Selects best exchange using ranking service, then validates it against registry.
     * Usage: protects callers from empty quotes list or misconfigured exchange codes.
     */
    private String pickOrDefault(List<VenueQuote> quotes, OrderSide side) {
        String chosen = rankingService.bestExchange(quotes, side);

        if (chosen == null || chosen.isBlank() || !exchangeRegistry.isSupported(chosen)) {
            return exchangeRegistry.normalizeOrDefault(null);
        }
        return chosen;
    }

    /**
     * Normalized routing inputs.
     * Usage: created once at entry to keep comparisons and repository queries consistent.
     */
    public record RouteRequest(String instrument, OrderSide side, OrderType type, BigDecimal limitPrice, long qty) {
        /**
         * Converts instrument into canonical representation used in DB + routing.
         * Usage: always call instead of constructing RouteRequest directly.
         */
        public static RouteRequest of(String instrument, OrderSide side, OrderType type, BigDecimal limitPrice, long qty) {
            String inst = String.valueOf(instrument).trim().toUpperCase(Locale.ROOT);
            return new RouteRequest(inst, side, type, limitPrice, qty);
        }
    }
}