package com.ab.orderservice.router;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RouteDecision {
    private String chosenExchange;
    private String reason;
    private List<VenueQuote> quotes;
}
