package com.ab.orderservice.dto.orderbook;

import com.ab.orderservice.dto.OrderResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderBookResponse {
    private String instrument;
    private String exchange;
    private boolean aggregated;
    private List<OrderResponse> bids; // BUY
    private List<OrderResponse> asks; // SELL
}
