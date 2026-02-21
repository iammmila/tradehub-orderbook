package com.ab.orderservice.orders.repository;

import com.ab.orderservice.orders.model.Order;
import com.ab.orderservice.orders.model.enums.OrderSide;
import com.ab.orderservice.orders.model.enums.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class OrderSpecifications {

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

            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));

            if (side != null) predicates.add(criteriaBuilder.equal(root.get("side"), side));

            if (instrument != null && !instrument.isBlank())
                predicates.add(criteriaBuilder.equal(root.get("instrument"), instrument.trim()));

            if (status != null) predicates.add(criteriaBuilder.equal(root.get("status"), status));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
