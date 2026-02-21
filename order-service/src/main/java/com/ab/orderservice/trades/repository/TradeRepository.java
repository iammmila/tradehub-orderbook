package com.ab.orderservice.trades.repository;

import com.ab.orderservice.trades.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByBuyOrder_User_IdOrSellOrder_User_Id(Long buyerId, Long sellerId);

    List<Trade> findByInstrument(String instrument);

    List<Trade> findByInstrumentAndBuyOrder_User_IdOrInstrumentAndSellOrder_User_Id(
            String instrument1, Long buyerId,
            String instrument2, Long sellerId
    );
}
