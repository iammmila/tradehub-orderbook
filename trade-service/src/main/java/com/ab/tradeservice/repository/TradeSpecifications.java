package com.ab.tradeservice.repository;

import com.ab.tradeservice.model.Trade;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TradeSpecifications {
    public static Specification<Trade> myTrades(Long userId, String instrument) {
        return (
                root, //entity fields
                query, //query structure
                criteriaBuilder //build conditions
        ) -> {
            List<Predicate> predicates = new ArrayList<>();

            Predicate isBuyer = criteriaBuilder.equal(root.get("buyerUserId"), userId);
            Predicate isSeller = criteriaBuilder.equal(root.get("sellerUserId"), userId);
            predicates.add(criteriaBuilder.or(isBuyer, isSeller));

            if (instrument != null && !instrument.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("instrument"), instrument.trim()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
