package com.ab.orderservice.service.matching;

import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MatchCandidateFinder {

    private final OrderRepository orderRepository;

    /**
     * Returns resting orders sorted by price priority then FIFO.
     */
    public List<Order> findCandidates(Order incoming) {
        String ex = incoming.getExchangeCode();
        String instrument = incoming.getInstrument();

        if (incoming.getSide() == OrderSide.BUY) {
            // BUY matches against SELL (best sell = lowest price)
            return orderRepository
                    .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
                            ex,
                            instrument,
                            OrderSide.SELL,
                            OrderNormalizer.getACTIVE_STATUSES(),
                            0L
                    );
        }

        // SELL matches against BUY (best buy = highest price)
        return orderRepository
                .findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
                        ex,
                        instrument,
                        OrderSide.BUY,
                        OrderNormalizer.getACTIVE_STATUSES(),
                        0L
                );
    }
}