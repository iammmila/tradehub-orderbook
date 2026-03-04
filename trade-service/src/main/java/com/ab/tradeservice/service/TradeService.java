package com.ab.tradeservice.service;

import com.ab.tradeservice.dto.CreateTradeRequest;
import com.ab.tradeservice.dto.TradeResponse;
import com.ab.tradeservice.mapper.TradeMapper;
import com.ab.tradeservice.model.Trade;
import com.ab.tradeservice.repository.TradeRepository;
import com.ab.tradeservice.repository.TradeSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Business layer for creating and querying trades.
 * Used by Kafka consumers and HTTP controllers to keep persistence and mapping logic centralized.
 */
@Service
@RequiredArgsConstructor
public class TradeService {
    private final TradeRepository tradeRepository;

    public void createTrade(CreateTradeRequest req) {
        Trade trade = Trade.builder()
                .instrument(req.getInstrument())
                .price(req.getPrice())
                .quantity(req.getQuantity())
                .buyOrderId(req.getBuyOrderId())
                .sellOrderId(req.getSellOrderId())
                .buyerUserId(req.getBuyerUserId())
                .sellerUserId(req.getSellerUserId())
                .exchangeCode(req.getExchangeCode() == null ? null : req.getExchangeCode().trim().toUpperCase())
                .createdAt(req.getCreatedAt() != null ? req.getCreatedAt() : LocalDateTime.now())
                .build();
        // fail fast: exchangeCode is part of the trade identity/routing context
        if (trade.getExchangeCode() == null || trade.getExchangeCode().isBlank()) {
            throw new IllegalArgumentException("exchangeCode is required to create trade");
        }
        tradeRepository.save(trade);
    }

    public Page<TradeResponse> getMyTradesPaged(
            Long userId,
            String instrument,
            Pageable pageable
    ) { // one place to keep query rules consistent (user scope + optional instrument filter)
        var specification = TradeSpecifications.myTrades(userId, instrument);
        return tradeRepository
                .findAll(specification, pageable)
                .map(TradeMapper::toResponse); // isolate API shape from entity shape
    }

    public List<TradeResponse> getTrades(String instrument) {
        String inst = (instrument == null || instrument.isBlank())
                ? null
                : instrument.trim();

        // null = no filter; keep behavior explicit and predictable
        if (inst == null) {
            return tradeRepository
                    .findAll()
                    .stream()
                    .map(TradeMapper::toResponse)
                    .toList();
        }

        return tradeRepository
                .findByInstrument(inst)
                .stream()
                .map(TradeMapper::toResponse)
                .toList();
    }
}
