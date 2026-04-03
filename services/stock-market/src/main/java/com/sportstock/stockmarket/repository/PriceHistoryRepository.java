package com.sportstock.stockmarket.repository;

import com.sportstock.common.enums.stock_market.PriceType;
import com.sportstock.stockmarket.model.entity.PriceHistory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, UUID> {

  List<PriceHistory> findByStockIdAndSeasonYearAndSeasonTypeOrderByWeekAsc(
      UUID stockId, int seasonYear, int seasonType);

  Optional<PriceHistory> findByStockIdAndSeasonYearAndSeasonTypeAndWeekAndPriceType(
      UUID stockId, int seasonYear, int seasonType, int week, PriceType priceType);

  @Modifying
  @Query(
      """
      UPDATE PriceHistory ph SET ph.price = :price, ph.recordedAt = CURRENT_TIMESTAMP
      WHERE ph.stock.id = :stockId AND ph.seasonYear = :seasonYear
        AND ph.seasonType = :seasonType AND ph.week = :week AND ph.priceType = :priceType
      """)
  int updatePrice(
      @Param("stockId") UUID stockId,
      @Param("seasonYear") int seasonYear,
      @Param("seasonType") int seasonType,
      @Param("week") int week,
      @Param("priceType") PriceType priceType,
      @Param("price") java.math.BigDecimal price);
}
