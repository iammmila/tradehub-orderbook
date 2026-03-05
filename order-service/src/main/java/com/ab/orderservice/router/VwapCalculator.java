package com.ab.orderservice.router;

import com.ab.orderservice.model.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class VwapCalculator {

    /**
     * Computes BUY VWAP by consuming SELL orders from best price upward.
     * Usage: taker BUY estimation for MARKET and crossing LIMIT orders.
     */
    public VwapResult computeBuy(List<Order> sellsAsc, long desiredQty, BigDecimal limitOrNull) {
        long remaining = desiredQty;
        BigDecimal notional = BigDecimal.ZERO;
        long filled = 0;

        for (Order s : sellsAsc) {
            if (remaining <= 0) break;
            if (limitOrNull != null && s.getPrice().compareTo(limitOrNull) > 0) break;

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

    /**
     * Computes SELL VWAP by consuming BUY orders from best price downward.
     * Usage: taker SELL estimation for MARKET and crossing LIMIT orders.
     */
    public VwapResult computeSell(List<Order> buysDesc, long desiredQty, BigDecimal limitOrNull) {
        long remaining = desiredQty;
        BigDecimal notional = BigDecimal.ZERO;
        long filled = 0;

        for (Order b : buysDesc) {
            if (remaining <= 0) break;
            if (limitOrNull != null && b.getPrice().compareTo(limitOrNull) < 0) break;

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

    public record VwapResult(long filled, BigDecimal vwap) {
    }
}
