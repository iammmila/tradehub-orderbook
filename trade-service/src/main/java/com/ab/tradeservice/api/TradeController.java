package com.ab.tradeservice.api;

import com.ab.tradeservice.dto.CreateTradeRequest;
import com.ab.tradeservice.dto.TradeResponse;
import com.ab.tradeservice.security.AuthPrincipal;
import com.ab.tradeservice.service.TradeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/trades")
public class TradeController {
    private final TradeService tradeService;

    // Called by order-service via Feign
    @PostMapping
    public ResponseEntity<Void> createTrade(@Valid @RequestBody CreateTradeRequest request) {
        tradeService.createTrade(request);
        return ResponseEntity.noContent().build();
    }

    // GET /api/v1/trades/my?instrument=BT -> 200
    // e.g. api/v1/trades/my?page=0&size=10&sort=createdAt,desc
    //    desc = descending = newest first
    //    asc = ascending = oldest first
    @GetMapping("/my")
    public ResponseEntity<Page<TradeResponse>> getMyTrades(
            @AuthenticationPrincipal AuthPrincipal me,
            @RequestParam(required = false) String instrument,
            Pageable pageable
    ) {
        return ResponseEntity.ok(tradeService.getMyTradesPaged(me.userId(), instrument, pageable));
    }

    // optional: GET /api/v1/trades?instrument=BT -> 200 (ADMIN)
    @GetMapping
    public ResponseEntity<List<TradeResponse>> getTrades(
            @RequestParam(required = false) String instrument
    ) {
        return ResponseEntity.ok(tradeService.getTrades(instrument));
    }
}
