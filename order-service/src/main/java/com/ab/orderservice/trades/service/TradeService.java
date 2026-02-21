package com.ab.orderservice.trades.service;

import com.ab.orderservice.trades.dto.TradeResponse;
import com.ab.orderservice.trades.model.Trade;
import com.ab.orderservice.trades.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeService {
    private final TradeRepository tradeRepository;

    public List<TradeResponse> getMyTrades(Long userId, String instrument) {
        List<Trade> trades;
        String inst = (instrument == null || instrument.isBlank())
                ? null
                : instrument.trim();


        if (inst == null) {
            trades = tradeRepository.findByBuyOrder_User_IdOrSellOrder_User_Id(userId, userId);
        } else {
            trades = tradeRepository
                    .findByInstrumentAndBuyOrder_User_IdOrInstrumentAndSellOrder_User_Id(
                            inst, userId, inst, userId
                    );
        }

        return trades.stream().map(this::toResponse).toList();
    }

    public List<TradeResponse> getTrades(String instrument) {
        String inst = (instrument == null || instrument.isBlank())
                ? null
                : instrument.trim();

        if (inst == null) {
            return tradeRepository
                    .findAll()
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        return tradeRepository
                .findByInstrument(inst)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TradeResponse toResponse(Trade t) {
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
