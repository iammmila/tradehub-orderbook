package com.ab.orderservice.service.order.support;

import com.ab.orderservice.exception.BadRequestException;
import com.ab.orderservice.exception.enums.ErrorCode;
import com.ab.orderservice.model.enums.*;
import com.ab.orderservice.router.SmartOrderRouter;
import com.ab.orderservice.service.ExchangeRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class OrderRoutingService {
    private final ExchangeRegistry exchangeRegistry;
    private final SmartOrderRouter smartOrderRouter;

    public RoutingDecision resolve(
            String requestedExchangeCode,
            String instrument,
            OrderSide side,
            OrderType type,
            BigDecimal limitPrice,
            Long quantity
    ) {
        if (requestedExchangeCode != null && !requestedExchangeCode.isBlank()) {
            String norm = requestedExchangeCode.trim().toUpperCase();
            if (!exchangeRegistry.isSupported(norm)) {
                throw new BadRequestException(ErrorCode.EXCHANGE_NOT_SUPPORTED);
            }
            return RoutingDecision.manual(norm);
        }

        var decision = smartOrderRouter.route(instrument, side, type, limitPrice, quantity);

        String chosen = decision != null ? decision.getChosenExchange() : null;
        chosen = exchangeRegistry.normalizeOrDefault(chosen);

        String reason = null;
        try {
            reason = (decision != null) ? decision.getReason() : null;
        } catch (Exception ignored) {
            reason = null;
        }

        return RoutingDecision.auto(chosen, reason);
    }

    public record RoutingDecision(
            String exchangeCode,
            RoutingMode routingMode,
            RoutedBy routedBy,
            String routeReason
    ) {
        public static RoutingDecision manual(String exchangeCode) {
            return new RoutingDecision(exchangeCode, RoutingMode.MANUAL, RoutedBy.USER, null);
        }

        public static RoutingDecision auto(String exchangeCode, String reason) {
            return new RoutingDecision(exchangeCode, RoutingMode.AUTO, RoutedBy.SOR, reason);
        }
    }
}