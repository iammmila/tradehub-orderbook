package com.ab.orderservice.router;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FeeCalculator {

    /**
     * Computes effective BUY price after fees.
     * Usage: used in ranking where lower effective price is preferred for BUY.
     */
    public BigDecimal effectiveBuy(BigDecimal px, int feeBps) {
        if (px == null) return null;
        BigDecimal fee = bpsToRate(feeBps);
        return px.multiply(BigDecimal.ONE.add(fee));
    }

    /**
     * Computes effective SELL price after fees.
     * Usage: used in ranking where higher effective price is preferred for SELL.
     */
    public BigDecimal effectiveSell(BigDecimal px, int feeBps) {
        if (px == null) return null;
        BigDecimal fee = bpsToRate(feeBps);
        return px.multiply(BigDecimal.ONE.subtract(fee));
    }

    /**
     * Converts basis points into decimal rate.
     * Usage: centralizes fee conversion to keep precision consistent.
     */
    private BigDecimal bpsToRate(int bps) {
        return BigDecimal.valueOf(bps)
                .divide(BigDecimal.valueOf(10_000), 8, RoundingMode.HALF_UP);
    }
}
