package com.ab.orderservice.orders.repository;

import com.ab.orderservice.orders.model.Order;
import com.ab.orderservice.orders.model.enums.OrderSide;
import com.ab.orderservice.orders.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findBySide(OrderSide side);

    List<Order> findByInstrument(String instrument);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findBySideAndInstrument(OrderSide side, String instrument);

    List<Order> findBySideAndStatus(OrderSide side, OrderStatus status);

    List<Order> findByInstrumentAndStatus(String instrument, OrderStatus status);

    List<Order> findBySideAndInstrumentAndStatus(OrderSide side, String instrument, OrderStatus status);

    List<Order> findByUser_Id(Long userId);

    List<Order> findByUser_IdAndSide(Long userId, OrderSide side);

    List<Order> findByUser_IdAndInstrument(Long userId, String instrument);

    List<Order> findByUser_IdAndStatus(Long userId, OrderStatus status);

    List<Order> findByUser_IdAndSideAndInstrument(Long userId, OrderSide side, String instrument);

    List<Order> findByUser_IdAndSideAndStatus(Long userId, OrderSide side, OrderStatus status);

    List<Order> findByUser_IdAndInstrumentAndStatus(Long userId, String instrument, OrderStatus status);

    List<Order> findByUser_IdAndSideAndInstrumentAndStatus(Long userId, OrderSide side, String instrument, OrderStatus status);

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
}
