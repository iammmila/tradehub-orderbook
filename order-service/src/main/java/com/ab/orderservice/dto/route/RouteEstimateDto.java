package com.ab.orderservice.dto.route;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RouteEstimateDto {
    private String exchange;
    private Long fillQuantity;
    private BigDecimal vwap;          // before fee
    private BigDecimal effectiveVwap; // after taker fee
    private String reason;
}