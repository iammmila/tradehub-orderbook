package com.ab.orderservice.trades.api;

import com.ab.orderservice.auth.userdetails.CustomUserDetails;
import com.ab.orderservice.trades.dto.TradeResponse;
import com.ab.orderservice.trades.service.TradeService;
import lombok.RequiredArgsConstructor;
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
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<TradeResponse>> getMyTrades(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String instrument
    ) {
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(tradeService.getMyTrades(userId, instrument));
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
