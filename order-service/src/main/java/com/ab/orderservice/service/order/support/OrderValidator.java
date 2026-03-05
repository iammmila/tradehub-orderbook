package com.ab.orderservice.service.order.support;

import com.ab.orderservice.dto.CreateOrderRequest;
import com.ab.orderservice.exception.BadRequestException;
import com.ab.orderservice.exception.enums.ErrorCode;
import com.ab.orderservice.model.enums.OrderType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderValidator {

    // Normalizes instrument to a consistent internal format.
    public String normalizeInstrument(String instrument) {
        if (instrument == null || instrument.isBlank()) {
            throw new BadRequestException(ErrorCode.INSTRUMENT_REQUIRED);
        }
        return instrument.trim().toUpperCase();
    }

    // Default type if not provided.
    public OrderType resolveType(CreateOrderRequest request) {
        return request.getType() != null ? request.getType() : OrderType.LIMIT;
    }

    // Applies price rules based on order type.
    public BigDecimal resolvePrice(OrderType type, BigDecimal requestPrice) {
        if (type == OrderType.MARKET) {
            // Client must not send price
            if (requestPrice != null) {
                throw new BadRequestException(ErrorCode.PRICE_NOT_ALLOWED_FOR_MARKET);
            }
            // Internal safe value (prevents DB NOT NULL / NPE issues)
            return BigDecimal.ZERO;
        }
        if (requestPrice == null || requestPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(ErrorCode.ORDER_PRICE_REQUIRED);
        }
        return requestPrice;
    }


    public Long resolveMinExecSize(OrderType type, Long minExecSize, Long quantity) {
        if (type != OrderType.MIN_EXECUTION_SIZE) {
            if (minExecSize != null) {
                throw new BadRequestException(ErrorCode.MIN_EXEC_SIZE_NOT_ALLOWED);
            }
            return null;
        }

        if (minExecSize == null || minExecSize <= 0) {
            throw new BadRequestException(ErrorCode.MIN_EXEC_SIZE_REQUIRED);
        }

        if (quantity != null && minExecSize > quantity) {
            throw new BadRequestException(ErrorCode.MIN_EXEC_SIZE_TOO_LARGE);
        }

        return minExecSize;
    }
}