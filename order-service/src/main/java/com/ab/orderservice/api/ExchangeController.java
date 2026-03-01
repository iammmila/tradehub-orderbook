package com.ab.orderservice.api;

import com.ab.orderservice.dto.exchange.ExchangeInfo;
import com.ab.orderservice.service.ExchangeRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/exchanges")
public class ExchangeController {

    private final ExchangeRegistry registry;

    @GetMapping
    public ResponseEntity<List<ExchangeInfo>> list() {
        return ResponseEntity.ok(registry.list());
    }
}