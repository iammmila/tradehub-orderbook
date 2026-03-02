package com.ab.orderservice.api;

import com.ab.orderservice.dto.CreateOrderRequest;
import com.ab.orderservice.dto.OrderResponse;
import com.ab.orderservice.dto.ReplaceOrderRequest;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.security.AuthPrincipal;
import com.ab.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;

    // get /api/v1/orders -> 200 ok
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(
            @AuthenticationPrincipal AuthPrincipal me,
            @RequestParam(required = false) OrderSide side,
            @RequestParam(required = false) String instrument,
            @RequestParam(required = false) OrderStatus status
    ) {
        return ResponseEntity.ok(orderService.getOrders(side, instrument, status));
    }

    // GET /api/v1/orders/my -> 200 OK
    // e.g. api/v1/orders/my?page=0&size=10&sort=createdAt,desc
    //    desc = descending = newest first
    //    asc = ascending = oldest first
    @GetMapping("/my")
    public ResponseEntity<Page<OrderResponse>> getOrdersByUser(
            @AuthenticationPrincipal AuthPrincipal me,
            @RequestParam(required = false) OrderSide side,
            @RequestParam(required = false) String instrument,
            @RequestParam(required = false) OrderStatus status,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                orderService.getOrdersByUserPaged(me.userId(), side, instrument, status, pageable));
    }

    // delete /api/v1/orders/orderid -> 200 OK
    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderResponse> cancelOrder(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long orderId
    ) {
        boolean isAdmin = false;
        return ResponseEntity.ok(orderService.cancelOrder(orderId, me.userId(), isAdmin));
    }

    // PATCH /api/v1/orders/{orderId} -> 200 OK
    @PatchMapping("/{orderId}")
    public ResponseEntity<OrderResponse> replaceOrder(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long orderId,
            @Valid @RequestBody ReplaceOrderRequest request
    ) {
        boolean isAdmin = false;
        return ResponseEntity.ok(orderService.replaceOrder(orderId, me.userId(), isAdmin, request));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @AuthenticationPrincipal AuthPrincipal me,
            @PathVariable Long orderId
    ) {
        boolean isAdmin = false;
        return ResponseEntity.ok(orderService.getOrderById(orderId, me.userId(), isAdmin));
    }

    // post /api/v1/orders -> 201
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal AuthPrincipal me,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderResponse created = orderService.createOrder(me.userId(), request);
        URI location = URI.create("/api/v1/orders/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }
}
