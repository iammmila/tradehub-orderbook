package com.ab.orderservice.service;

import com.ab.orderservice.model.Order;
import com.ab.orderservice.repository.OrderRepository;
import com.ab.orderservice.service.matching.MatchCandidateFinder;
import com.ab.orderservice.service.matching.MatchingEngine;
import com.ab.orderservice.service.matching.OrderNormalizer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class MatchingService {
    private final OrderRepository orderRepository;
    private final OrderNormalizer normalizer;
    private final MatchCandidateFinder candidateFinder;
    private final MatchingEngine engine;

    @Transactional
    public void match(Order incoming) {
        if (!normalizer.isMatchable(incoming)) {
            return;
        }

        normalizer.normalizeOrThrow(incoming);

        var candidates = candidateFinder.findCandidates(incoming);

        Set<Order> touchedRestingOrders = engine.match(incoming, candidates);

        // Persist updated entities
        touchedRestingOrders.forEach(orderRepository::save);
        orderRepository.save(incoming);
    }
}
