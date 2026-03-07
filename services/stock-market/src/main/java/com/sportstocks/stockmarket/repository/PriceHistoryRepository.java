package com.sportstocks.stockmarket.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sportstocks.stockmarket.model.entity.PriceHistory;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

    List<PriceHistory> findByPlayerStockIdAndSeasonYearAndSeasonTypeOrderByWeekAsc(
            UUID playerStockId, int seasonYear, int seasonType);
}