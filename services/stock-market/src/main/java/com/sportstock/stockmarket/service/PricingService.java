package com.sportstock.stockmarket.service;

import com.sportstock.common.dto.ingestion.FantasySnapshotResponse;
import com.sportstock.common.enums.stock_market.PriceType;
import com.sportstock.common.enums.stock_market.StockStatus;
import com.sportstock.common.enums.stock_market.StockType;
import com.sportstock.stockmarket.client.IngestionApiClient;
import com.sportstock.stockmarket.config.PricingConfig;
import com.sportstock.stockmarket.model.entity.PriceHistory;
import com.sportstock.stockmarket.model.entity.Stock;
import com.sportstock.stockmarket.repository.PriceHistoryRepository;
import com.sportstock.stockmarket.repository.StockRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingService {

  private final IngestionApiClient ingestionApiClient;
  private final StockRepository stockRepository;
  private final PriceHistoryRepository priceHistoryRepository;
  private final PricingConfig pricingConfig;

  @Transactional
  public PriceUpdateResult updateProjectedPrices(int seasonYear, int seasonType, int weekNumber) {
    log.info(
            "Updating projected prices for season {} type {} week {}",
            seasonYear,
            seasonType,
            weekNumber);

    List<Stock> activeStocks = stockRepository.findByStatusAndGameLockedFalse(StockStatus.ACTIVE);
    int updated = 0;
    int skipped = 0;

    for (Stock stock : activeStocks) {
      FantasySnapshotResponse snapshot =
              ingestionApiClient.getFantasySnapshot(
                      stock.getEspnId(), seasonYear, seasonType, weekNumber);

      if (snapshot == null || snapshot.projectedFantasyPoints() == null) {
        skipped++;
        continue;
      }

      BigDecimal multiplier = getMultiplier(stock.getPosition());
      BigDecimal newPrice = computePrice(snapshot.projectedFantasyPoints(), multiplier);

      stock.setCurrentPrice(newPrice);
      stock.setPriceUpdatedAt(Instant.now());

      upsertPriceHistory(stock, seasonYear, seasonType, weekNumber, newPrice, PriceType.BASE);
      updated++;
    }

    return new PriceUpdateResult(updated, skipped);
  }

  @Transactional
  public PriceUpdateResult updateFinalPrices(String eventEspnId) {
    log.info("Updating final prices for event {}", eventEspnId);

    List<FantasySnapshotResponse> snapshots =
            ingestionApiClient.getFantasySnapshotsByEvent(eventEspnId);

    int updated = 0;
    int skipped = 0;

    for (FantasySnapshotResponse snapshot : snapshots) {
      if (snapshot.actualFantasyPoints() == null) {
        skipped++;
        continue;
      }

      Stock stock =
              stockRepository
                      .findByEspnIdAndType(
                              snapshot.espnId(),
                              "TEAM_DEFENSE".equals(snapshot.subjectType())
                                      ? StockType.TEAM_DEFENSE
                                      : StockType.PLAYER)
                      .orElse(null);

      if (stock == null) {
        skipped++;
        continue;
      }

      BigDecimal multiplier = getMultiplier(stock.getPosition());
      BigDecimal newPrice = computePrice(snapshot.actualFantasyPoints(), multiplier);

      stock.setCurrentPrice(newPrice);
      stock.setPriceUpdatedAt(Instant.now());

      // Parse season/week from the snapshot's event (need to get from ingestion)
      // For now, use the event data from the snapshot
      var eventInfo = ingestionApiClient.getEvent(eventEspnId);
      if (eventInfo != null) {
        insertFinalPriceHistory(
                stock,
                eventInfo.getSeasonYear(),
                eventInfo.getSeasonType(),
                eventInfo.getWeekNumber(),
                newPrice);
      }
      updated++;
    }

    return new PriceUpdateResult(updated, skipped);
  }

  private BigDecimal computePrice(BigDecimal fantasyPoints, BigDecimal multiplier) {
    BigDecimal price =
            fantasyPoints.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    BigDecimal floor = pricingConfig.getPriceFloor();
    if (floor != null && price.compareTo(floor) < 0) {
      return floor;
    }
    return price;
  }

  private BigDecimal getMultiplier(String position) {
    if (pricingConfig.getMultipliers() == null) {
      return BigDecimal.ONE;
    }
    return pricingConfig.getMultipliers().getOrDefault(position, BigDecimal.ONE);
  }

  private void upsertPriceHistory(
          Stock stock,
          int seasonYear,
          int seasonType,
          int week,
          BigDecimal price,
          PriceType priceType) {
    int updatedRows =
            priceHistoryRepository.updatePrice(
                    stock.getId(), seasonYear, seasonType, week, priceType, price);

    if (updatedRows == 0) {
      PriceHistory history = new PriceHistory();
      history.setStock(stock);
      history.setSeasonYear(seasonYear);
      history.setSeasonType(seasonType);
      history.setWeek(week);
      history.setPrice(price);
      history.setPriceType(priceType);
      priceHistoryRepository.save(history);
    }
  }

  private void insertFinalPriceHistory(
          Stock stock, int seasonYear, int seasonType, int week, BigDecimal price) {
    PriceHistory history = new PriceHistory();
    history.setStock(stock);
    history.setSeasonYear(seasonYear);
    history.setSeasonType(seasonType);
    history.setWeek(week);
    history.setPrice(price);
    history.setPriceType(PriceType.FINAL);
    priceHistoryRepository.save(history);
  }

  public record PriceUpdateResult(int updated, int skipped) {
  }

}