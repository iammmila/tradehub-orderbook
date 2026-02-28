package com.ab.orderservice.api;

import com.ab.orderservice.dto.orderbook.OrderBookResponse;
import com.ab.orderservice.security.AuthPrincipal;
import com.ab.orderservice.service.OrderBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orderbook")
public class OrderBookController {
    private final OrderBookService orderBookService;

    /**
     * GET /api/v1/orderbook?instrument=BTS
     * Requires JWT (authenticated user).
     */
    @GetMapping
    public ResponseEntity<OrderBookResponse> getBook(
            @AuthenticationPrincipal AuthPrincipal me,
            @RequestParam String instrument
    ) {
        // "me" will be null only if SecurityConfig allowed anonymous access.
        // With anyRequest().authenticated(), it will never be null.
        return ResponseEntity.ok(orderBookService.getOrderBook(instrument));
    }
}
