package com.ab.orderservice.orders.api;

import com.ab.orderservice.auth.userdetails.CustomUserDetails;
import com.ab.orderservice.orders.dto.CreateOrderRequest;
import com.ab.orderservice.orders.dto.OrderResponse;
import com.ab.orderservice.orders.dto.ReplaceOrderRequest;
import com.ab.orderservice.orders.model.enums.OrderSide;
import com.ab.orderservice.orders.model.enums.OrderStatus;
import com.ab.orderservice.orders.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(
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
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) OrderSide side,
            @RequestParam(required = false) String instrument,
            @RequestParam(required = false) OrderStatus status,
            Pageable pageable
    ) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(orderService.getOrdersByUserPaged(userId, side, instrument, status, pageable));
    }

    // delete /api/v1/orders/orderid -> 200 OK
    @DeleteMapping("/{orderId}")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentUserId = userDetails.getUser().getId();
        boolean isAdmin = userDetails
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return ResponseEntity.ok(orderService.cancelOrder(orderId, currentUserId, isAdmin));
    }

    // PATCH /api/v1/orders/{orderId} -> 200 OK
    @PatchMapping("/{orderId}")
    public ResponseEntity<OrderResponse> replaceOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReplaceOrderRequest request
    ) {
        Long currentUserId = userDetails.getUser().getId();
        boolean isAdmin = userDetails
                .getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return ResponseEntity.ok(orderService.replaceOrder(orderId, currentUserId, isAdmin, request));
    }

    // post /api/v1/orders -> 201
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        Long userId = userDetails.getId();
        OrderResponse created = orderService.createOrder(userId, request);

        URI location = URI.create("/api/v1/orders/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }
}
