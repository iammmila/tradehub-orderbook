package com.ab.orderservice.router;

import com.ab.orderservice.model.enums.OrderSide;

import java.util.List;

public interface VenueRankingService {
    /**
     * Returns venues sorted by routing preference.
     * Usage: used by plan() endpoint and for consistent logging.
     */
    List<VenueQuote> rank(List<VenueQuote> quotes, OrderSide side);

    /**
     * Returns best exchange code using the same ranking rule.
     * Usage: used by route() to select final venue.
     */
    String bestExchange(List<VenueQuote> quotes, OrderSide side);
}