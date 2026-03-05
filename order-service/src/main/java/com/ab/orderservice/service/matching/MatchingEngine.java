package com.ab.orderservice.service.matching;

import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MatchingEngine {
    private final MatchRules rules;
    private final OrderFillApplier fillApplier;
    private final MatchingEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Matches incoming order against already-sorted resting candidates.
     * Returns the set of resting orders that were changed and must be saved.
     */
    public Set<Order> match(Order incoming, List<Order> candidates) {
        Set<Order> touched = new LinkedHashSet<>();

        for (Order resting : candidates) {
            if (incoming.getRemainingQuantity() <= 0) break;
            if (!rules.canMatch(incoming, resting)) continue;

            long tradeQty = Math.min(incoming.getRemainingQuantity(), resting.getRemainingQuantity());
            BigDecimal tradePrice = rules.tradePrice(incoming, resting);
            LocalDateTime now = LocalDateTime.now(clock);

            Order buy = incoming.getSide() == OrderSide.BUY ? incoming : resting;
            Order sell = incoming.getSide() == OrderSide.SELL ? incoming : resting;

            eventPublisher.publishTradeCreated(buy, sell, tradePrice, tradeQty, now);

            // apply fills
            fillApplier.applyFill(incoming, tradeQty);
            fillApplier.applyFill(resting, tradeQty);

            // order fill events
            eventPublisher.publishOrderFillIfNeeded(incoming, tradeQty);
            eventPublisher.publishOrderFillIfNeeded(resting, tradeQty);

            touched.add(resting);
        }

        return touched;
    }
}