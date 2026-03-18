package com.sportstock.stockmarket.repository;

import com.sportstock.stockmarket.model.entity.PriceHistory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

  List<PriceHistory> findByPlayerStockIdAndSeasonYearAndSeasonTypeOrderByWeekAsc(
      UUID playerStockId, int seasonYear, int seasonType);

  Optional<PriceHistory> findByPlayerStockIdAndSeasonYearAndSeasonTypeAndWeek(
      UUID playerStockId, int seasonYear, int seasonType, int week);
}
