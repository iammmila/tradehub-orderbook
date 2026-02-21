package com.ab.orderservice.orders.dto;

import com.ab.orderservice.orders.model.enums.OrderSide;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateOrderRequest {

    @NotBlank
    private String instrument;

    @NotNull
    private OrderSide side;

    @Positive
    @NotNull
    private BigDecimal price;

    @NotNull
    @Positive
    private Long quantity;
}
