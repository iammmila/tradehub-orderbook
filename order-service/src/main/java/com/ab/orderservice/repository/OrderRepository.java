package com.ab.orderservice.repository;

import com.ab.orderservice.model.Order;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    List<Order> findByInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
            String instrument,
            OrderSide side,
            List<OrderStatus> statuses,
            Long remainingQuantity
    );

    List<Order> findByInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
            String instrument,
            OrderSide side,
            List<OrderStatus> statuses,
            Long remainingQuantity
    );

    List<Order> findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
            String exchangeCode,
            String instrument,
            OrderSide side,
            List<OrderStatus> statuses,
            Long remainingQuantity
    );

    List<Order> findByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
            String exchangeCode,
            String instrument,
            OrderSide side,
            List<OrderStatus> statuses,
            Long remainingQuantity
    );

    Optional<Order> findFirstByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceAscCreatedAtAsc(
            String exchangeCode,
            String instrument,
            OrderSide side,
            List<OrderStatus> statuses,
            Long remainingQuantity
    );

    Optional<Order> findFirstByExchangeCodeAndInstrumentAndSideAndStatusInAndRemainingQuantityGreaterThanOrderByPriceDescCreatedAtAsc(
            String exchangeCode,
            String instrument,
            OrderSide side,
            List<OrderStatus> statuses,
            Long remainingQuantity
    );
}
