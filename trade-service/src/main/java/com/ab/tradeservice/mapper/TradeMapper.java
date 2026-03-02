package com.ab.tradeservice.mapper;

import com.ab.tradeservice.dto.TradeResponse;
import com.ab.tradeservice.model.Trade;

public final class TradeMapper {

    private TradeMapper() {
    }

    public static TradeResponse toResponse(Trade t) {
        return TradeResponse.builder()
                .id(t.getId())
                .instrument(t.getInstrument())
                .price(t.getPrice())
                .quantity(t.getQuantity())
                .buyOrderId(t.getBuyOrderId())
                .sellOrderId(t.getSellOrderId())
                .buyerUserId(t.getBuyerUserId())
                .sellerUserId(t.getSellerUserId())
                .exchangeCode(t.getExchangeCode())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
