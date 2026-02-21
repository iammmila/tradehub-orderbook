package com.ab.orderservice.trades.api;

import com.ab.orderservice.auth.userdetails.CustomUserDetails;
import com.ab.orderservice.trades.dto.TradeResponse;
import com.ab.orderservice.trades.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trades")
public class TradeController {
    private final TradeService tradeService;

    // GET /api/v1/trades/my?instrument=BT -> 200
    // e.g. api/v1/trades/my?page=0&size=10&sort=createdAt,desc
    //    desc = descending = newest first
    //    asc = ascending = oldest first
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<Page<TradeResponse>> getMyTrades(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String instrument,
            Pageable pageable
    ) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(tradeService.getMyTradesPaged(userId, instrument, pageable));
    }

    // optional: GET /api/v1/trades?instrument=BT -> 200 (ADMIN)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TradeResponse>> getTrades(
            @RequestParam(required = false) String instrument
    ) {
        return ResponseEntity.ok(tradeService.getTrades(instrument));
    }
}
