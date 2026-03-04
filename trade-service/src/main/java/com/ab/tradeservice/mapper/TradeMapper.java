package com.ab.tradeservice.mapper;

import com.ab.tradeservice.dto.TradeResponse;
import com.ab.tradeservice.model.Trade;

/**
 * Maps Trade entity to API response DTO.
 * Usage:
 * - Prevents leaking JPA entities directly to the API layer.
 * - Keeps response shaping consistent and easy to evolve (add/remove fields without touching entity).
 */
public final class TradeMapper {

    private TradeMapper() {//utility class; prevent instantiation.
    }

    //explicit mapping is predictable and avoids accidental serialization of lazy relations.
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
