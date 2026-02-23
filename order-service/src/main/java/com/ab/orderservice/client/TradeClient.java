package com.ab.orderservice.client;

import com.ab.orderservice.client.dto.CreateTradeRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "TRADE-SERVICE")
public interface TradeClient {

    @PostMapping("/api/v1/trades")
    void createTrade(@RequestBody CreateTradeRequest request);
}
