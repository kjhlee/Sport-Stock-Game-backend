package com.sportstock.stockmarket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportstock.common.dto.stock_market.IngestionAthleteDto;
import com.sportstock.stockmarket.client.IngestionApiClient;
import com.sportstock.stockmarket.config.PricingConfig;
import com.sportstock.stockmarket.model.entity.Stock;
import com.sportstock.stockmarket.repository.StockRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockSyncService Unit Tests")
class StockSyncServiceTest {

  @Mock private IngestionApiClient ingestionApiClient;
  @Mock private StockRepository stockRepository;

  private StockSyncService stockSyncService;

  @BeforeEach
  void setUp() {
    PricingConfig pricingConfig = new PricingConfig();
    pricingConfig.setBasePrices(
        Map.of(
            "QB", new BigDecimal("10.00"),
            "RB", new BigDecimal("10.00"),
            "WR", new BigDecimal("10.00"),
            "TE", new BigDecimal("10.00"),
            "K", new BigDecimal("10.00"),
            "DST", new BigDecimal("8.00")));
    stockSyncService = new StockSyncService(ingestionApiClient, stockRepository, pricingConfig);
  }

  @Test
  @DisplayName("Should reject DST athlete sync filter")
  void shouldRejectDstAthleteSyncFilter() {
    assertThatThrownBy(() -> stockSyncService.syncAthletes("DST"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sync-team-defense");
  }

  @Test
  @DisplayName("Should skip DST athletes from the player stock sync path")
  void shouldSkipDstAthletesFromPlayerStockSyncPath() {
    IngestionAthleteDto dstAthlete = new IngestionAthleteDto();
    ReflectionTestUtils.setField(dstAthlete, "espnId", "16");
    ReflectionTestUtils.setField(dstAthlete, "fullName", "Jets D/ST");
    ReflectionTestUtils.setField(dstAthlete, "positionAbbreviation", "DST");
    ReflectionTestUtils.setField(dstAthlete, "statusType", "ACTIVE");
    ReflectionTestUtils.setField(dstAthlete, "teamEspnId", "20");

    when(ingestionApiClient.getAthletes(null)).thenReturn(List.of(dstAthlete));

    StockSyncService.SyncAthletesResult result = stockSyncService.syncAthletes(null);

    assertThat(result.created()).isEqualTo(0);
    assertThat(result.updated()).isEqualTo(0);
    assertThat(result.skipped()).isEqualTo(1);
    verify(stockRepository, never()).save(any(Stock.class));
  }

  @Test
  @DisplayName("Should create player stocks for supported athlete positions")
  void shouldCreatePlayerStocksForSupportedAthletePositions() {
    IngestionAthleteDto quarterback = new IngestionAthleteDto();
    ReflectionTestUtils.setField(quarterback, "espnId", "7");
    ReflectionTestUtils.setField(quarterback, "fullName", "Test Quarterback");
    ReflectionTestUtils.setField(quarterback, "positionAbbreviation", "QB");
    ReflectionTestUtils.setField(quarterback, "statusType", "ACTIVE");
    ReflectionTestUtils.setField(quarterback, "teamEspnId", "18");

    when(ingestionApiClient.getAthletes("QB")).thenReturn(List.of(quarterback));
    when(stockRepository.findByEspnIdAndType("7", com.sportstock.common.enums.stock_market.StockType.PLAYER))
        .thenReturn(Optional.empty());

    StockSyncService.SyncAthletesResult result = stockSyncService.syncAthletes("QB");

    assertThat(result.created()).isEqualTo(1);
    assertThat(result.skipped()).isEqualTo(0);
    verify(stockRepository).save(any(Stock.class));
  }
}
