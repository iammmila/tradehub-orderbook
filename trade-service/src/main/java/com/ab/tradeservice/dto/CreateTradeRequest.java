package com.ab.tradeservice.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTradeRequest {
    private String instrument;
    private BigDecimal price;
    private Long quantity;

    private Long buyOrderId;
    private Long sellOrderId;

    private Long buyerUserId;
    private Long sellerUserId;

    private String exchangeCode;

    private LocalDateTime createdAt;
}