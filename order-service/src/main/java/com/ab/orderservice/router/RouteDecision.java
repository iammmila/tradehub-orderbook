package com.ab.orderservice.router;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RouteDecision {
    // Final venue selected by router (normalized exchange code)
    private String chosenExchange;

    // Human-readable decision summary taken from the chosen venue quote
    private String reason;

    // All venue quotes used in decision, useful for diagnostics and testing
    private List<VenueQuote> quotes;
}
