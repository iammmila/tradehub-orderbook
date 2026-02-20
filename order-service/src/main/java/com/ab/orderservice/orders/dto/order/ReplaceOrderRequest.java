package com.ab.orderservice.orders.dto.order;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReplaceOrderRequest {

    @Positive
    private BigDecimal price;

    @Positive
    private Long quantity;
}
