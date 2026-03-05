package com.ab.orderservice.router;

import com.ab.orderservice.dto.exchange.ExchangeInfo;
import com.ab.orderservice.model.enums.OrderSide;

import java.math.BigDecimal;

public final class QuoteReasonBuilder {
    private QuoteReasonBuilder() {
    }

    /**
     * Builds a short explanation describing the quote characteristics.
     * Usage: stored in VenueQuote and returned to UI / logs.
     */
    public static String build(
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
}
