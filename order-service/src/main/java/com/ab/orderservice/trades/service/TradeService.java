package com.ab.orderservice.trades.service;

import com.ab.orderservice.trades.dto.TradeResponse;
import com.ab.orderservice.trades.mapper.TradeMapper;
import com.ab.orderservice.trades.repository.TradeRepository;
import com.ab.orderservice.trades.repository.TradeSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeService {
    private final TradeRepository tradeRepository;

    public Page<TradeResponse> getMyTradesPaged(
            Long userId,
            String instrument,
            Pageable pageable
    ) {
        var specification = TradeSpecifications.myTrades(userId, instrument);
        return tradeRepository
                .findAll(specification, pageable)
                .map(TradeMapper::toResponse);
    }

    public List<TradeResponse> getTrades(String instrument) {
        String inst = (instrument == null || instrument.isBlank())
                ? null
                : instrument.trim();

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
