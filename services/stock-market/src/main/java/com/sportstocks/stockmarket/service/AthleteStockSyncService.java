package com.sportstocks.stockmarket.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sportstocks.stockmarket.client.IngestionApiClient;
import com.sportstocks.stockmarket.config.PricingConfig;
import com.sportstocks.stockmarket.dto.IngestionAthleteDto;
import com.sportstocks.stockmarket.model.entity.PlayerStock;
import com.sportstocks.stockmarket.model.enums.StockStatus;
import com.sportstocks.stockmarket.repository.PlayerStockRepository;

@Service
public class AthleteStockSyncService {

    private static final Set<String> SUPPORTED_POSITIONS = Set.of("QB", "RB", "WR", "TE", "K");

    private final IngestionApiClient ingestionApiClient;
    private final PlayerStockRepository playerStockRepository;
    private final PricingConfig pricingConfig;

    public AthleteStockSyncService(
            IngestionApiClient ingestionApiClient,
            PlayerStockRepository playerStockRepository,
            PricingConfig pricingConfig
    ) {
        this.ingestionApiClient = ingestionApiClient;
        this.playerStockRepository = playerStockRepository;
        this.pricingConfig = pricingConfig;
    }

    @Transactional
    public SyncAthletesResult syncAthletes(String position) {
        String ingestionFilterPosition = normalizeRequestedPositionForIngestion(position);

        List<IngestionAthleteDto> athletes = ingestionApiClient.getAthletes(ingestionFilterPosition);

        if (athletes.isEmpty()) {
            return new SyncAthletesResult(0, 0, 0, 0);
        }

        int created = 0;
        int updated = 0;
        int skipped = 0;

        for (IngestionAthleteDto athlete : athletes) {
            String normalizedAthletePosition = normalizeAthletePosition(athlete.getPositionAbbreviation());

            if (!isSupportedPosition(normalizedAthletePosition)) {
                skipped++;
                continue;
            }

            boolean wasCreated = upsertStockFromAthlete(athlete, normalizedAthletePosition);
            if (wasCreated) {
                created++;
            } else {
                updated++;
            }
        }

        return new SyncAthletesResult(created, updated, skipped, athletes.size());
    }

    private boolean upsertStockFromAthlete(IngestionAthleteDto athlete, String normalizedPosition) {
        String athleteEspnId = athlete.getEspnId();

        PlayerStock existing = playerStockRepository.findByAthleteEspnId(athleteEspnId).orElse(null);

        if (existing != null) {
            existing.setFullName(athlete.getFullName());
            existing.setPosition(normalizedPosition);
            existing.setTeamEspnId(athlete.getTeamEspnId());
            existing.setStatus(resolveStatus(athlete));
            return false;
        }

        PlayerStock stock = new PlayerStock();
        stock.setAthleteEspnId(athleteEspnId);
        stock.setFullName(athlete.getFullName());
        stock.setPosition(normalizedPosition);
        stock.setTeamEspnId(athlete.getTeamEspnId());
        stock.setCurrentPrice(resolveInitialPrice(normalizedPosition));
        stock.setStatus(resolveStatus(athlete));

        playerStockRepository.save(stock);
        return true;
    }

    /**
     * Normalizes the optional incoming filter from the stock-market API
     * to the value expected by the ingestion service.
     *
     * Examples:
     * - null -> null (no filter)
     * - "qb" -> "QB"
     * - "k"  -> "PK" (ingestion uses PK)
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
     * Normalizes athlete positions coming back from ingestion
     * into the stock-market domain positions.
     *
     * Example:
     * - "PK" -> "K"
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
            return StockStatus.ACTIVE;
        }

        String normalized = statusType.trim().toUpperCase();

        return switch (normalized) {
            case "ACTIVE" -> StockStatus.ACTIVE;
            case "INACTIVE" -> StockStatus.INACTIVE;
            default -> StockStatus.ACTIVE;
        };
    }

    public record SyncAthletesResult(int created, int updated, int skipped, int totalFetched) {
    }
}