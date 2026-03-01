package com.ab.orderservice.router;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class VenueQuote {
    private String exchangeCode;

    // top of book
    private BigDecimal bestBid; // null if no bids
    private BigDecimal bestAsk; // null if no asks

    private boolean takerNow;
    private Long estimatedFillQty;
    private BigDecimal estimatedExecPx; // estimated price
    private BigDecimal effectivePrice;  // includes fees (BUY: higher is worse, SELL: lower is worse)

    private int makerFeeBps;
    private int takerFeeBps;

    private String reason;
}