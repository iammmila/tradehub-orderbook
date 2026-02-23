package com.ab.orderservice.api;

import com.ab.orderservice.dto.orderbook.OrderBookResponse;
import com.ab.orderservice.security.RoleUtil;
import com.ab.orderservice.service.OrderBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orderbook")
public class OrderBookController {
    private final OrderBookService orderBookService;

    // GET /api/v1/orderbook?instrument=BTS
    @GetMapping
    public ResponseEntity<OrderBookResponse> getBook(
            @RequestHeader(value = "X-Roles", required = false) String roles,
            @RequestParam String instrument
    ) {
        if (!RoleUtil.hasAnyRole(roles, "ROLE_USER", "ROLE_ADMIN")) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(orderBookService.getOrderBook(instrument));
    }
}
