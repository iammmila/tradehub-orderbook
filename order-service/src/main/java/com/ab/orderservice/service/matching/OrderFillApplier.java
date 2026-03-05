package com.ab.orderservice.service.matching;


import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderStatus;
import org.springframework.stereotype.Component;

@Component
public class OrderFillApplier {

    /**
     * Applies filled quantity and updates order status.
     */
    public void applyFill(Order order, long filledNow) {
        long rem = order.getRemainingQuantity() == null ? 0L : order.getRemainingQuantity();
        rem = rem - filledNow;

        if (rem <= 0) {
            order.setRemainingQuantity(0L);
            order.setStatus(OrderStatus.FILLED);
            return;
        }

        order.setRemainingQuantity(rem);

        if (order.getQuantity() != null && rem < order.getQuantity()) {
            order.setStatus(OrderStatus.PARTIALLY_FILLED);
        } else {
            order.setStatus(OrderStatus.NEW);
        }
    }
}
