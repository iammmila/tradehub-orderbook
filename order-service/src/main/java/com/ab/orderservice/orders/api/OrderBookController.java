package com.ab.orderservice.orders.api;

import com.ab.orderservice.orders.dto.orderbook.OrderBookResponse;
import com.ab.orderservice.orders.service.OrderBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orderbook")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class OrderBookController {
    private final OrderBookService orderBookService;

    // GET /api/v1/orderbook?instrument=BTS
    @GetMapping
    public ResponseEntity<OrderBookResponse> getBook(@RequestParam String instrument) {
        return ResponseEntity.ok(orderBookService.getOrderBook(instrument));
    }
}
