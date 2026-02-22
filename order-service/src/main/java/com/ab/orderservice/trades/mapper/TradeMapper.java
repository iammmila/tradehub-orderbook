package com.ab.orderservice.trades.mapper;

import com.ab.orderservice.trades.dto.TradeResponse;
import com.ab.orderservice.trades.model.Trade;

public final class TradeMapper {

    private TradeMapper() {
    }

    public static TradeResponse toResponse(Trade t) {
        if (t == null) return null;

        return TradeResponse.builder()
                .id(t.getId())
                .instrument(t.getInstrument())
                .price(t.getPrice())
                .quantity(t.getQuantity())
                .buyOrderId(t.getBuyOrder().getId())
                .sellOrderId(t.getSellOrder().getId())
                .buyerUserId(t.getBuyOrder().getUser().getId())
                .sellerUserId(t.getSellOrder().getUser().getId())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
