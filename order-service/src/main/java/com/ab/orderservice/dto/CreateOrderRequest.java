package com.ab.orderservice.dto;

import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderRequest {

    @NotBlank
    private String instrument;

    @NotNull
    private OrderSide side;

    @NotNull
    private OrderType type;

    @DecimalMin(value = "0", inclusive = true)
    private BigDecimal price;

    @NotNull
    @Positive
    private Long quantity;

    @PositiveOrZero
    private Long minExecSize;

    private String exchangeCode;

    // Custom validation rule:
    @AssertTrue(message = "price must be > 0 for LIMIT / HIDDEN_LIMIT / MIN_EXECUTION_SIZE orders")
    public boolean isPriceValid() {
        if (type == null) return true;

        if (type == OrderType.MARKET) {
            // MARKET ignores price
            return true;
        }

        // For all non-market types, require positive price
        return price != null && price.compareTo(BigDecimal.ZERO) > 0;
    }
    // MARKET must not send price
    @AssertTrue(message = "price must be omitted (null) for MARKET orders")
    public boolean isMarketPriceAbsent() {
        if (type == null) return true;
        if (type != OrderType.MARKET) return true;
        return price == null;
    }

    // minExecSize allowed only when type == MIN_EXECUTION_SIZE
    @AssertTrue(message = "minExecSize is allowed only for MIN_EXECUTION_SIZE orders")
    public boolean isMinExecSizeAllowedOnlyForMinExecOrders() {
        if (type == null) return true;
        if (type == OrderType.MIN_EXECUTION_SIZE) return true;
        return minExecSize == null;
    }

    // For MIN_EXECUTION_SIZE, minExecSize is required and must be <= quantity
    @AssertTrue(message = "minExecSize must be > 0 and <= quantity for MIN_EXECUTION_SIZE orders")
    public boolean isMinExecSizeValidWhenRequired() {
        if (type == null) return true;
        if (type != OrderType.MIN_EXECUTION_SIZE) return true;

        if (minExecSize == null) return false;
        if (minExecSize <= 0) return false;

        if (quantity == null) return true; // quantity validation covers null
        return minExecSize <= quantity;
    }
}
