package com.ab.orderservice.router;

import com.ab.orderservice.model.enums.OrderSide;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class VenueRankingServiceImpl implements VenueRankingService {

    /**
     * Sort order used across router:
     * - prioritize higher estimated fill
     * - compare effective price (BUY prefers lower, SELL prefers higher)
     * - tie-break by exchange code for stable output
     */
    @Override
    public List<VenueQuote> rank(List<VenueQuote> quotes, OrderSide side) {
        List<VenueQuote> sorted = new ArrayList<>(quotes);
        sorted.sort(comparator(side));
        return sorted;
    }

    /**
     * Picks the top venue according to the same comparator used by rank().
     * Usage: keeps route() and plan() selection consistent.
     */
    @Override
    public String bestExchange(List<VenueQuote> quotes, OrderSide side) {
        return quotes.stream()
                .min(comparator(side))
                .map(VenueQuote::getExchangeCode)
                .orElse(null);
    }

    private Comparator<VenueQuote> comparator(OrderSide side) {
        return (a, b) -> {
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
        };
    }
}