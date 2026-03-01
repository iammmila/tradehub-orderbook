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

    @Positive
    @NotNull
    private BigDecimal price;

    @NotNull
    @Positive
    private Long quantity;

    @PositiveOrZero
    private Long minExecSize;

    private String exchangeCode;

    // Custom validation rule:
    @AssertTrue(message = "price is required for LIMIT/HIDDEN_LIMIT orders")
    public boolean isPriceValid() {
        if (type == null) return true;
        return switch (type) {
            case MARKET -> true;
            default -> price != null;
        };
    }
}
