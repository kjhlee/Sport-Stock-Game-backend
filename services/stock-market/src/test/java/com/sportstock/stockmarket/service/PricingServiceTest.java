package com.sportstock.stockmarket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportstock.common.dto.ingestion.FantasySnapshotResponse;
import com.sportstock.common.enums.stock_market.StockStatus;
import com.sportstock.common.enums.stock_market.StockType;
import com.sportstock.common.dto.stock_market.IngestionAthleteDto;
import com.sportstock.common.dto.stock_market.IngestionEventDto;
import com.sportstock.stockmarket.client.IngestionApiClient;
import com.sportstock.stockmarket.config.PricingConfig;
import com.sportstock.stockmarket.model.entity.Stock;
import com.sportstock.stockmarket.repository.PriceHistoryRepository;
import com.sportstock.stockmarket.repository.StockRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PricingService Unit Tests")
class PricingServiceTest {

  @Mock private IngestionApiClient ingestionApiClient;
  @Mock private StockRepository stockRepository;
  @Mock private PriceHistoryRepository priceHistoryRepository;

  private PricingService pricingService;

  @BeforeEach
  void setUp() {
    PricingConfig pricingConfig = new PricingConfig();
    pricingConfig.setPriceFloor(new BigDecimal("1.00"));
    pricingConfig.setMultipliers(
        java.util.Map.of("QB", new BigDecimal("0.50"), "DST", new BigDecimal("1.20")));
    pricingService =
        new PricingService(
            ingestionApiClient, stockRepository, priceHistoryRepository, pricingConfig);
  }

  @Test
  @DisplayName("Should use stock type when fetching projected fantasy snapshots")
  void shouldUseStockTypeWhenFetchingProjectedFantasySnapshots() {
    Stock teamDefense = new Stock();
    teamDefense.setEspnId("5");
    teamDefense.setType(StockType.TEAM_DEFENSE);
    teamDefense.setPosition("DST");
    teamDefense.setStatus(StockStatus.ACTIVE);
    teamDefense.setCurrentPrice(new BigDecimal("8.00"));

    FantasySnapshotResponse snapshot =
        new FantasySnapshotResponse(
            1L,
            "401000001",
            "TEAM_DEFENSE",
            "5",
            "Cleveland Browns D/ST",
            "{\"sacks\":3}",
            new BigDecimal("10.00"),
            null,
            false,
            Instant.now());

    when(stockRepository.findByStatusAndGameLockedFalse(StockStatus.ACTIVE))
        .thenReturn(List.of(teamDefense));
    when(ingestionApiClient.getFantasySnapshot("5", "TEAM_DEFENSE", 2026, 2, 1))
        .thenReturn(snapshot);
    when(priceHistoryRepository.updatePrice(
            eq(teamDefense.getId()), eq(2026), eq(2), eq(1), eq(com.sportstock.common.enums.stock_market.PriceType.BASE), eq(new BigDecimal("12.00"))))
        .thenReturn(1);

    PricingService.PriceUpdateResult result = pricingService.updateProjectedPrices(2026, 2, 1);

    assertThat(result.updated()).isEqualTo(1);
    assertThat(result.skipped()).isEqualTo(0);
    assertThat(teamDefense.getCurrentPrice()).isEqualByComparingTo("12.00");
    verify(ingestionApiClient).getFantasySnapshot("5", "TEAM_DEFENSE", 2026, 2, 1);
  }

  @Test
  @DisplayName("Should create missing player stock during final price update")
  void shouldCreateMissingPlayerStockDuringFinalPriceUpdate() {
    FantasySnapshotResponse snapshot =
        new FantasySnapshotResponse(
            1L,
            "401000001",
            "PLAYER",
            "3139477",
            "Lamar Jackson",
            null,
            null,
            new BigDecimal("20.00"),
            true,
            Instant.now());

    IngestionEventDto event = new IngestionEventDto("401000001", 2026, 2, 1, true, "post");

    IngestionAthleteDto athlete = new IngestionAthleteDto();
    ReflectionTestUtils.setField(athlete, "espnId", "3139477");
    ReflectionTestUtils.setField(athlete, "fullName", "Lamar Jackson");
    ReflectionTestUtils.setField(athlete, "positionAbbreviation", "QB");
    ReflectionTestUtils.setField(athlete, "statusType", "ACTIVE");
    ReflectionTestUtils.setField(athlete, "teamEspnId", "33");

    when(ingestionApiClient.getFantasySnapshotsByEvent("401000001")).thenReturn(List.of(snapshot));
    when(ingestionApiClient.getEvent("401000001")).thenReturn(event);
    when(stockRepository.findByEspnIdAndType("3139477", StockType.PLAYER))
        .thenReturn(java.util.Optional.empty());
    when(ingestionApiClient.getAthlete("3139477")).thenReturn(athlete);
    when(stockRepository.save(any(Stock.class)))
        .thenAnswer(
            invocation -> {
              Stock saved = invocation.getArgument(0);
              if (saved.getId() == null) {
                ReflectionTestUtils.setField(saved, "id", java.util.UUID.randomUUID());
              }
              return saved;
            });
    when(priceHistoryRepository.updatePrice(
            any(), eq(2026), eq(2), eq(1), eq(com.sportstock.common.enums.stock_market.PriceType.FINAL), eq(new BigDecimal("10.00"))))
        .thenReturn(1);

    PricingService.PriceUpdateResult result = pricingService.updateFinalPrices("401000001");

    assertThat(result.updated()).isEqualTo(1);
    assertThat(result.skipped()).isEqualTo(0);
    verify(stockRepository).save(any(Stock.class));
  }
}
