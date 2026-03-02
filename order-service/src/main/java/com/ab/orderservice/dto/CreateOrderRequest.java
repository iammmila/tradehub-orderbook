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
}
