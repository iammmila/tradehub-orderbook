package com.ab.orderservice.dto;

import com.ab.orderservice.model.enums.OrderSide;
import com.ab.orderservice.model.enums.OrderStatus;
import com.ab.orderservice.model.enums.OrderType;
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
    private OrderType type;
    private Long minExecSize;
    private Boolean visible;
    private Long userId;
    private String exchangeCode;
}
