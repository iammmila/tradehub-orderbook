package com.ab.orderservice.orders.dto.order;

import com.ab.orderservice.orders.model.enums.OrderSide;
import com.ab.orderservice.orders.model.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private Long id;
    private String instrument;
    private OrderSide side;
    private BigDecimal price;
    private Long quantity;
    private Long remainingQuantity;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private Long userId;
}
