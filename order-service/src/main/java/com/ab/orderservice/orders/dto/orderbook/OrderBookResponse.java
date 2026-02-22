package com.ab.orderservice.orders.dto.orderbook;

import com.ab.orderservice.orders.dto.OrderResponse;
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
    private List<OrderResponse> bids; // BUY
    private List<OrderResponse> asks; // SELL
}
