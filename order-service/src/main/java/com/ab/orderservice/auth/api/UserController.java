package com.ab.orderservice.auth.api;

import com.ab.orderservice.orders.dto.order.CreateOrderRequest;
import com.ab.orderservice.orders.dto.order.OrderResponse;
import com.ab.orderservice.auth.dto.user.UserResponse;
import com.ab.orderservice.orders.model.enums.OrderSide;
import com.ab.orderservice.orders.model.enums.OrderStatus;
import com.ab.orderservice.orders.service.OrderService;
import com.ab.orderservice.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@PreAuthorize("hasAnyRole('ROLE_USER','ROLE_ADMIN')")
public class UserController {
    private final UserService userService;
    private final OrderService orderService;

    // GET /api/v1/users -> 200 OK
    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers() {
        return ResponseEntity.ok(userService.getUsers());
    }

    // GET /api/v1/users/{id} -> 200 OK or 404
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    // GET /api/v1/users/{userid}/orders -> 200 OK
    @GetMapping("/{userId}/orders")
    public ResponseEntity<List<OrderResponse>> getOrdersByUser(
            @PathVariable Long userId,
            @RequestParam(required = false) OrderSide side,
            @RequestParam(required = false) String instrument,
            @RequestParam(required = false) OrderStatus status
    ) {
        return ResponseEntity.ok(orderService.getOrdersByUser(userId, side, instrument, status));
    }

    // post /api/v1/users/{userId}/orders -> 201
    @PostMapping("/{userId}/orders")
    public ResponseEntity<OrderResponse> createOrder(
            @PathVariable Long userId,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        OrderResponse created = orderService.createOrder(userId, request);

        URI location = URI.create("/api/v1/orders/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }
}
