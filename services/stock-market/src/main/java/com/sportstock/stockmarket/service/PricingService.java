package com.sportstock.stockmarket.service;

import com.sportstock.common.dto.stock_market.IngestionEventDto;
import com.sportstock.common.dto.stock_market.IngestionPlayerGameStatsDto;
import com.sportstock.stockmarket.client.IngestionApiClient;
import com.sportstock.stockmarket.config.PricingConfig;
import com.sportstock.stockmarket.model.entity.Stock;
import com.sportstock.stockmarket.model.entity.PriceHistory;
import com.sportstock.stockmarket.repository.StockRepository;
import com.sportstock.stockmarket.repository.PriceHistoryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class PricingService {

  private final IngestionApiClient ingestionApiClient;
  private final StockRepository stockRepository;
  private final PriceHistoryRepository priceHistoryRepository;
  private final PricingConfig pricingConfig;

  public PricingService(
      IngestionApiClient ingestionApiClient,
      StockRepository stockRepository,
      PriceHistoryRepository priceHistoryRepository,
      PricingConfig pricingConfig) {
    this.ingestionApiClient = ingestionApiClient;
    this.stockRepository = stockRepository;
    this.priceHistoryRepository = priceHistoryRepository;
    this.pricingConfig = pricingConfig;
  }

  @Transactional
  public PriceUpdateResult updatePricesForWeek(int seasonYear, int seasonType, int weekNumber) {
    log.info("Updating prices for season {} type {} week {}", seasonYear, seasonType, weekNumber);

    List<IngestionEventDto> events =
        ingestionApiClient.getEvents(seasonYear, seasonType, weekNumber);

    if (events.isEmpty()) {
      log.warn("No events found for season {} type {} week {}", seasonYear, seasonType, weekNumber);
      return new PriceUpdateResult(0, 0);
    }

    // Collect stats from all completed events, grouped by athleteEspnId
    Map<String, List<IngestionPlayerGameStatsDto>> statsByAthlete =
        events.stream()
            .filter(event -> Boolean.TRUE.equals(event.getStatusCompleted()))
            .flatMap(event -> ingestionApiClient.getPlayerStats(event.getEspnId()).stream())
            .collect(Collectors.groupingBy(IngestionPlayerGameStatsDto::getAthleteEspnId));

    int updated = 0;
    int skipped = 0;

    for (Map.Entry<String, List<IngestionPlayerGameStatsDto>> entry : statsByAthlete.entrySet()) {
      String athleteEspnId = entry.getKey();
      List<IngestionPlayerGameStatsDto> stats = entry.getValue();

      Stock stock = stockRepository.findByAthleteEspnId(athleteEspnId).orElse(null);
      if (stock == null) {
        log.debug("No stock found for athlete {}, skipping", athleteEspnId);
        skipped++;
        continue;
      }

      BigDecimal performanceScore = computePerformanceScore(stock.getPosition(), stats);
      BigDecimal newPrice = applySmoothing(stock.getCurrentPrice(), performanceScore);

      stock.setCurrentPrice(newPrice);
      stock.setPriceUpdatedAt(Instant.now());

      PriceHistory history =
          priceHistoryRepository
              .findByPlayerStockIdAndSeasonYearAndSeasonTypeAndWeek(
                  stock.getId(), seasonYear, seasonType, weekNumber)
              .orElseGet(
                  () -> {
                    PriceHistory h = new PriceHistory();
                    h.setStock(stock);
                    h.setSeasonYear(seasonYear);
                    h.setSeasonType(seasonType);
                    h.setWeek(weekNumber);
                    return h;
                  });
      history.setPrice(newPrice);
      priceHistoryRepository.save(history);

      log.info(
          "Updated price for athlete {} ({}) to {}", athleteEspnId, stock.getFullName(), newPrice);
      updated++;
    }

    return new PriceUpdateResult(updated, skipped);
  }

  private BigDecimal computePerformanceScore(
      String position, List<IngestionPlayerGameStatsDto> stats) {
    if (position == null) {
      return BigDecimal.ZERO;
    }
    return switch (position.toUpperCase()) {
      case "QB" -> scoreQb(stats);
      case "RB" -> scoreRb(stats);
      case "WR", "TE" -> scoreReceiver(stats);
      case "K" -> scoreKicker(stats);
      default -> BigDecimal.ZERO;
    };
  }

  private BigDecimal scoreQb(List<IngestionPlayerGameStatsDto> stats) {
    BigDecimal score = BigDecimal.ZERO;
    for (IngestionPlayerGameStatsDto stat : stats) {
      Map<String, String> s = stat.getStats();
      if (s == null) continue;
      switch (stat.getStatCategory().toLowerCase()) {
        case "passing" -> {
          score =
              score.add(
                  getStat(s, "passingYards")
                      .divide(BigDecimal.valueOf(25), 4, RoundingMode.HALF_UP));
          score = score.add(getStat(s, "passingTouchdowns").multiply(BigDecimal.valueOf(4)));
          score = score.subtract(getStat(s, "interceptions").multiply(BigDecimal.valueOf(2)));
        }
        case "rushing" -> {
          score =
              score.add(
                  getStat(s, "rushingYards")
                      .divide(BigDecimal.valueOf(10), 4, RoundingMode.HALF_UP));
          score = score.add(getStat(s, "rushingTouchdowns").multiply(BigDecimal.valueOf(6)));
        }
        default -> {}
      }
    }
    return score.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal scoreRb(List<IngestionPlayerGameStatsDto> stats) {
    BigDecimal score = BigDecimal.ZERO;
    for (IngestionPlayerGameStatsDto stat : stats) {
      Map<String, String> s = stat.getStats();
      if (s == null) continue;
      switch (stat.getStatCategory().toLowerCase()) {
        case "rushing" -> {
          score =
              score.add(
                  getStat(s, "rushingYards")
                      .divide(BigDecimal.valueOf(10), 4, RoundingMode.HALF_UP));
          score = score.add(getStat(s, "rushingTouchdowns").multiply(BigDecimal.valueOf(6)));
        }
        case "receiving" -> {
          score = score.add(getStat(s, "receptions"));
          score =
              score.add(
                  getStat(s, "receivingYards")
                      .divide(BigDecimal.valueOf(10), 4, RoundingMode.HALF_UP));
          score = score.add(getStat(s, "receivingTouchdowns").multiply(BigDecimal.valueOf(6)));
        }
        default -> {}
      }
    }
    return score.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal scoreReceiver(List<IngestionPlayerGameStatsDto> stats) {
    BigDecimal score = BigDecimal.ZERO;
    for (IngestionPlayerGameStatsDto stat : stats) {
      Map<String, String> s = stat.getStats();
      if (s == null) continue;
      if ("receiving".equalsIgnoreCase(stat.getStatCategory())) {
        score = score.add(getStat(s, "receptions"));
        score =
            score.add(
                getStat(s, "receivingYards")
                    .divide(BigDecimal.valueOf(10), 4, RoundingMode.HALF_UP));
        score = score.add(getStat(s, "receivingTouchdowns").multiply(BigDecimal.valueOf(6)));
      }
    }
    return score.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal scoreKicker(List<IngestionPlayerGameStatsDto> stats) {
    BigDecimal score = BigDecimal.ZERO;
    for (IngestionPlayerGameStatsDto stat : stats) {
      Map<String, String> s = stat.getStats();
      if (s == null) continue;
      if ("kicking".equalsIgnoreCase(stat.getStatCategory())) {
        BigDecimal fgMade = parseNumerator(s, "fieldGoalsMade/fieldGoalsAttempted");
        BigDecimal fgAttempted = parseDenominator(s, "fieldGoalsMade/fieldGoalsAttempted");
        BigDecimal fgMissed = fgAttempted.subtract(fgMade);
        BigDecimal xpMade = parseNumerator(s, "extraPointsMade/extraPointsAttempted");
        score = score.add(fgMade.multiply(BigDecimal.valueOf(3)));
        score = score.add(xpMade);
        score = score.subtract(fgMissed);
      }
    }
    return score.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * Applies exponential smoothing: newPrice = alpha * performanceScore + (1 - alpha) * currentPrice
   * Clamps result to priceFloor.
   */
  private BigDecimal applySmoothing(BigDecimal currentPrice, BigDecimal performanceScore) {
    BigDecimal alpha = pricingConfig.getSmoothingAlpha();
    BigDecimal floor = pricingConfig.getPriceFloor();

    BigDecimal newPrice =
        alpha
            .multiply(performanceScore)
            .add(BigDecimal.ONE.subtract(alpha).multiply(currentPrice))
            .setScale(2, RoundingMode.HALF_UP);

    if (floor != null && newPrice.compareTo(floor) < 0) {
      return floor;
    }

    return newPrice;
  }

  private BigDecimal getStat(Map<String, String> stats, String key) {
    String value = stats.get(key);
    if (value == null || value.isBlank()) {
      return BigDecimal.ZERO;
    }
    try {
      return new BigDecimal(value.trim());
    } catch (NumberFormatException e) {
      log.debug("Could not parse stat '{}' value '{}' as a number", key, value);
      return BigDecimal.ZERO;
    }
  }

  private BigDecimal parseNumerator(Map<String, String> stats, String key) {
    return parseFraction(stats, key, true);
  }

  private BigDecimal parseDenominator(Map<String, String> stats, String key) {
    return parseFraction(stats, key, false);
  }

  private BigDecimal parseFraction(Map<String, String> stats, String key, boolean numerator) {
    String value = stats.get(key);
    if (value == null || value.isBlank()) {
      return BigDecimal.ZERO;
    }
    String[] parts = value.trim().split("/");
    String target = numerator ? parts[0] : (parts.length > 1 ? parts[1] : parts[0]);
    try {
      return new BigDecimal(target.trim());
    } catch (NumberFormatException e) {
      log.debug(
          "Could not parse fraction part from stat '{}' value '{}' (numerator={})",
          key,
          value,
          numerator);
      return BigDecimal.ZERO;
    }
  }

  public record PriceUpdateResult(int updated, int skipped) {}
}
