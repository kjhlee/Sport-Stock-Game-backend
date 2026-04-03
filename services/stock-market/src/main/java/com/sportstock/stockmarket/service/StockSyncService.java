package com.sportstock.stockmarket.service;

import com.sportstock.common.dto.stock_market.IngestionAthleteDto;
import com.sportstock.common.dto.stock_market.IngestionTeamDto;
import com.sportstock.common.enums.stock_market.StockType;
import com.sportstock.stockmarket.client.IngestionApiClient;
import com.sportstock.stockmarket.config.PricingConfig;
import com.sportstock.stockmarket.model.entity.Stock;
import com.sportstock.common.enums.stock_market.StockStatus;
import com.sportstock.stockmarket.repository.StockRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class StockSyncService {

  private static final Set<String> SUPPORTED_POSITIONS = Set.of("QB", "RB", "WR", "TE", "K", "DST");

  private final IngestionApiClient ingestionApiClient;
  private final StockRepository stockRepository;
  private final PricingConfig pricingConfig;

  public StockSyncService(
      IngestionApiClient ingestionApiClient,
      StockRepository stockRepository,
      PricingConfig pricingConfig) {
    this.ingestionApiClient = ingestionApiClient;
    this.stockRepository = stockRepository;
    this.pricingConfig = pricingConfig;
  }

  @Transactional
  public SyncAthletesResult syncAthletes(String position) {
    String ingestionFilterPosition = normalizeRequestedPositionForIngestion(position);
    log.info("Starting Sync Athletes for position '{}'", ingestionFilterPosition);
    List<IngestionAthleteDto> athletes = ingestionApiClient.getAthletes(ingestionFilterPosition);

    if (athletes.isEmpty()) {
      return new SyncAthletesResult(0, 0, 0, 0);
    }

    int created = 0;
    int updated = 0;
    int skipped = 0;

    for (IngestionAthleteDto athlete : athletes) {
      String normalizedAthletePosition =
          normalizeAthletePosition(athlete.getPositionAbbreviation());

      if (!isSupportedPosition(normalizedAthletePosition)) {
        skipped++;
        log.warn(
            "Skipping athlete '{}' with unsupported position '{}'",
            athlete.getFullName(),
            normalizedAthletePosition);
        continue;
      }

      boolean wasCreated = upsertStockFromAthlete(athlete, normalizedAthletePosition);
      if (wasCreated) {
        created++;
        log.info(
            "Created stock for athlete '{}' with position '{}'",
            athlete.getFullName(),
            normalizedAthletePosition);
      } else {
        updated++;
        log.info(
            "Updated stock for athlete '{}' with position '{}'",
            athlete.getFullName(),
            normalizedAthletePosition);
      }
    }

    return new SyncAthletesResult(created, updated, skipped, athletes.size());
  }

  private boolean upsertStockFromAthlete(IngestionAthleteDto athlete, String normalizedPosition) {
    String espnId = athlete.getEspnId();

    Stock existing = stockRepository.findByEspnIdAndType(espnId, StockType.PLAYER).orElse(null);

    if (existing != null) {
      existing.setFullName(athlete.getFullName());
      existing.setPosition(normalizedPosition);
      existing.setTeamEspnId(athlete.getTeamEspnId());
      existing.setStatus(resolveStatus(athlete));
      return false;
    }

    Stock stock = new Stock();
    stock.setEspnId(espnId);
    stock.setType(StockType.PLAYER);
    stock.setFullName(athlete.getFullName());
    stock.setPosition(normalizedPosition);
    stock.setTeamEspnId(athlete.getTeamEspnId());
    stock.setCurrentPrice(resolveInitialPrice(normalizedPosition));
    stock.setStatus(resolveStatus(athlete));

    stockRepository.save(stock);
    return true;
  }

  @Transactional
  public SyncAthletesResult syncTeamDefenseStocks() {
    log.info("Syncing team defense stocks");
    List<IngestionTeamDto> teams = ingestionApiClient.getTeams();

    int created = 0;
    int updated = 0;

    for (IngestionTeamDto team : teams) {
      String espnId = team.getEspnId();
      Stock existing = stockRepository.findByEspnIdAndType(espnId, StockType.TEAM_DEFENSE).orElse(null);

      if (existing != null) {
        existing.setFullName(team.getDisplayName() + " D/ST");
        existing.setTeamEspnId(espnId);
        updated++;
        continue;
      }

      Stock stock = new Stock();
      stock.setEspnId(espnId);
      stock.setType(StockType.TEAM_DEFENSE);
      stock.setFullName(team.getDisplayName() + " D/ST");
      stock.setPosition("DST");
      stock.setTeamEspnId(espnId);
      stock.setCurrentPrice(resolveInitialPrice("DST"));
      stock.setStatus(StockStatus.ACTIVE);
      stockRepository.save(stock);
      created++;
    }

    return new SyncAthletesResult(created, updated, 0, teams.size());
  }

  /**
   * Normalizes the optional incoming filter from the stock-market API to the value expected by the
   * ingestion service.
   *
   * <p>Examples: - null -> null (no filter) - "qb" -> "QB" - "k" -> "PK" (ingestion uses PK)
   */
  private String normalizeRequestedPositionForIngestion(String rawPosition) {
    if (rawPosition == null || rawPosition.isBlank()) {
      return null;
    }

    String normalized = rawPosition.trim().toUpperCase();

    if ("K".equals(normalized)) {
      return "PK";
    }

    return normalized;
  }

  /**
   * Normalizes athlete positions coming back from ingestion into the stock-market domain positions.
   *
   * <p>Example: - "PK" -> "K"
   */
  private String normalizeAthletePosition(String rawPosition) {
    if (rawPosition == null || rawPosition.isBlank()) {
      return null;
    }

    String normalized = rawPosition.trim().toUpperCase();

    if ("PK".equals(normalized)) {
      return "K";
    }

    return normalized;
  }

  private boolean isSupportedPosition(String position) {
    return position != null && SUPPORTED_POSITIONS.contains(position);
  }

  private BigDecimal resolveInitialPrice(String position) {
    if (pricingConfig.getBasePrices() == null || pricingConfig.getBasePrices().isEmpty()) {
      throw new IllegalStateException("pricing.base-prices is not configured");
    }

    BigDecimal price = pricingConfig.getBasePrices().get(position);
    if (price == null) {
      throw new IllegalArgumentException("No base price configured for position: " + position);
    }

    return price;
  }

  private StockStatus resolveStatus(IngestionAthleteDto athlete) {
    String statusType = athlete.getStatusType();

    if (statusType == null || statusType.isBlank()) {
      return StockStatus.DELISTED;
    }

    String normalized = statusType.trim().toUpperCase();

    return switch (normalized) {
      case "ACTIVE" -> StockStatus.ACTIVE;
      case "DELISTED" -> StockStatus.DELISTED;
      default -> StockStatus.ACTIVE;
    };
  }

  public record SyncAthletesResult(int created, int updated, int skipped, int totalFetched) {}
}
