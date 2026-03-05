package com.ab.orderservice.service.matching;

import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MatchRules {

    /**
     * Checks if incoming can match with resting from a basic rules perspective.
     */
    public boolean canMatch(Order incoming, Order resting) {
        if (incoming.getRemainingQuantity() <= 0) return false;
        if (resting.getRemainingQuantity() == null || resting.getRemainingQuantity() <= 0) return false;

        // prevent self-trade
        if (incoming.getUserId() != null && incoming.getUserId().equals(resting.getUserId())) {
            return false;
        }

        // min execution size check
        long qty = Math.min(incoming.getRemainingQuantity(), resting.getRemainingQuantity());
        if (!satisfiesMinExec(incoming.getMinExecSize(), resting.getMinExecSize(), qty)) {
            return false;
        }

        // price check for limit orders
        if (!isMarket(incoming)) {
            // BUY: incoming price must be >= resting sell price
            if (incoming.getSide() == OrderSide.BUY) {
                if (incoming.getPrice() == null || resting.getPrice() == null) return false;
                if (incoming.getPrice().compareTo(resting.getPrice()) < 0) return false;
            } else {
                // SELL: incoming price must be <= resting buy price
                if (incoming.getPrice() == null || resting.getPrice() == null) return false;
                if (resting.getPrice().compareTo(incoming.getPrice()) < 0) return false;
            }
        }

        return true;
    }

    public boolean isMarket(Order order) {
        return order.getType() == OrderType.MARKET;
    }

    public BigDecimal tradePrice(Order incoming, Order resting) {
        // trade price = resting order price
        return resting.getPrice();
    }

    private boolean satisfiesMinExec(Long incomingMin, Long restingMin, long tradeQty) {
        if (incomingMin != null && tradeQty < incomingMin) return false;
        if (restingMin != null && tradeQty < restingMin) return false;
        return true;
    }
}