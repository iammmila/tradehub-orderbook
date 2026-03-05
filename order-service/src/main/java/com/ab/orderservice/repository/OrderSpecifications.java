package com.ab.orderservice.repository;

import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable JPA Specifications for filtering Orders in repository queries.
 * Keeps query-building logic out of services/controllers.
 */
public class OrderSpecifications {
    /**
     * Optional filters for searching orders (side, instrument, status).
     * Any null/blank parameter is ignored.
     */
    public static Specification<Order> withFilters(
            OrderSide side,
            String instrument,
            OrderStatus status
    ) {
        return (
                root, //entity fields
                query, //query structure
                criteriaBuilder //build conditions
        ) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (side != null) {
                predicates.add(criteriaBuilder.equal(root.get("side"), side));
            }

            if (instrument != null && !instrument.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("instrument"), instrument.trim()));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Filters orders for a single user, plus optional side/instrument/status.
     * Used for "my orders" endpoints.
     */
    public static Specification<Order> byUserAndFilters(
            Long userId,
            OrderSide side,
            String instrument,
            OrderStatus status
    ) {
        return (
                root, //entity fields
                query, //query structure
                criteriaBuilder //build conditions
        ) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));

            if (side != null) predicates.add(criteriaBuilder.equal(root.get("side"), side));

            if (instrument != null && !instrument.isBlank())
                predicates.add(criteriaBuilder.equal(root.get("instrument"), instrument.trim()));

            if (status != null) predicates.add(criteriaBuilder.equal(root.get("status"), status));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
