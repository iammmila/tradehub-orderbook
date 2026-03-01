package com.ab.orderservice.api;

import com.ab.orderservice.dto.route.RoutePlanResponse;
import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderType;
import com.ab.orderservice.router.RouteDecision;
import com.ab.orderservice.router.SmartOrderRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/route")
public class RoutingController {
    private final SmartOrderRouter router;

    @GetMapping("/quote")
    public ResponseEntity<RouteDecision> quote(
            @RequestParam String instrument,
            @RequestParam OrderSide side,
            @RequestParam OrderType type,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam long quantity
    ) {
        RouteDecision decision = router.route(instrument, side, type, price, quantity);
        return ResponseEntity.ok(decision);
    }

    @GetMapping("/plan")
    public ResponseEntity<RoutePlanResponse> plan(
            @RequestParam String instrument,
            @RequestParam OrderSide side,
            @RequestParam OrderType type,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam long quantity
    ) {
        return ResponseEntity.ok(router.plan(instrument, side, type, price, quantity));
    }
}