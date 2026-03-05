package com.ab.orderservice.router;

import com.ab.orderservice.dto.exchange.ExchangeInfo;

public interface VenueQuoteService {
    /**
     * Creates a venue quote for a single exchange.
     * Usage: called by SmartOrderRouter for each configured venue.
     */
    VenueQuote quote(String exchangeCode, ExchangeInfo info, SmartOrderRouter.RouteRequest req);
}
