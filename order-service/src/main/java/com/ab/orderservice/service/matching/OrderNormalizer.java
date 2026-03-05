package com.ab.orderservice.service.matching;

import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderStatus;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderNormalizer {

    @Getter
    private static final List<OrderStatus> ACTIVE_STATUSES =
            List.of(OrderStatus.NEW, OrderStatus.PARTIALLY_FILLED);

    /**
     * Checks if the order can be matched.
     */
    public boolean isMatchable(Order incoming) {
        if (incoming == null) return false;
        if (incoming.getRemainingQuantity() == null || incoming.getRemainingQuantity() <= 0) return false;
        return ACTIVE_STATUSES.contains(incoming.getStatus());
    }

    /**
     * Normalizes fields used by matching + validates required values.
     */
    public void normalizeOrThrow(Order incoming) {
        if (incoming.getInstrument() != null) {
            incoming.setInstrument(incoming.getInstrument().trim().toUpperCase());
        }

        if (incoming.getExchangeCode() == null || incoming.getExchangeCode().isBlank()) {
            throw new IllegalStateException("Incoming order has null exchangeCode");
        }
        incoming.setExchangeCode(incoming.getExchangeCode().trim().toUpperCase());
    }
}
