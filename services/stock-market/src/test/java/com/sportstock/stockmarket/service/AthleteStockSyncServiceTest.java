package com.sportstock.stockmarket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportstock.common.dto.stock_market.IngestionAthleteDto;
import com.sportstock.stockmarket.client.IngestionApiClient;
import com.sportstock.stockmarket.config.PricingConfig;
import com.sportstock.stockmarket.model.entity.PlayerStock;
import com.sportstock.stockmarket.model.enums.StockStatus;
import com.sportstock.stockmarket.repository.PlayerStockRepository;
import com.sportstock.stockmarket.service.AthleteStockSyncService.SyncAthletesResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AthleteStockSyncServiceTest {

  @Mock private IngestionApiClient ingestionApiClient;
  @Mock private PlayerStockRepository playerStockRepository;
  @Mock private PricingConfig pricingConfig;

  @InjectMocks private AthleteStockSyncService syncService;

  @BeforeEach
  void setUp() {
    when(pricingConfig.getBasePrices())
        .thenReturn(
            Map.of(
                "QB", new BigDecimal("15.00"),
                "RB", new BigDecimal("12.00"),
                "WR", new BigDecimal("10.00"),
                "TE", new BigDecimal("8.00"),
                "K", new BigDecimal("5.00")));
  }

  // --- helper ---

  private IngestionAthleteDto athlete(
      String espnId, String name, String position, String status, String teamId) {
    IngestionAthleteDto dto = new IngestionAthleteDto();
    ReflectionTestUtils.setField(dto, "espnId", espnId);
    ReflectionTestUtils.setField(dto, "fullName", name);
    ReflectionTestUtils.setField(dto, "positionAbbreviation", position);
    ReflectionTestUtils.setField(dto, "statusType", status);
    ReflectionTestUtils.setField(dto, "teamEspnId", teamId);
    return dto;
  }

  // --- empty list ---

  @Test
  void syncAthletes_emptyAthleteList_returnsZeroCounts() {
    when(ingestionApiClient.getAthletes(null)).thenReturn(List.of());

    SyncAthletesResult result = syncService.syncAthletes(null);

    assertThat(result.created()).isZero();
    assertThat(result.updated()).isZero();
    assertThat(result.skipped()).isZero();
    assertThat(result.totalFetched()).isZero();
    verify(playerStockRepository, never()).save(any());
  }

  // --- create ---

  @Test
  void syncAthletes_newAthlete_createsStockWithBasePrice() {
    when(ingestionApiClient.getAthletes(null))
        .thenReturn(List.of(athlete("qb-1", "Lamar Jackson", "QB", "ACTIVE", "33")));
    when(playerStockRepository.findByAthleteEspnId("qb-1")).thenReturn(Optional.empty());
    when(playerStockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    SyncAthletesResult result = syncService.syncAthletes(null);

    assertThat(result.created()).isEqualTo(1);
    assertThat(result.updated()).isZero();

    ArgumentCaptor<PlayerStock> captor = forClass(PlayerStock.class);
    verify(playerStockRepository).save(captor.capture());
    PlayerStock saved = captor.getValue();
    assertThat(saved.getAthleteEspnId()).isEqualTo("qb-1");
    assertThat(saved.getFullName()).isEqualTo("Lamar Jackson");
    assertThat(saved.getPosition()).isEqualTo("QB");
    assertThat(saved.getCurrentPrice()).isEqualByComparingTo("15.00");
    assertThat(saved.getStatus()).isEqualTo(StockStatus.ACTIVE);
  }

  // --- update ---

  @Test
  void syncAthletes_existingAthlete_updatesFieldsWithoutChangingPrice() {
    PlayerStock existing = new PlayerStock();
    existing.setAthleteEspnId("qb-1");
    existing.setFullName("Old Name");
    existing.setPosition("QB");
    existing.setCurrentPrice(new BigDecimal("22.50"));
    existing.setStatus(StockStatus.ACTIVE);

    when(ingestionApiClient.getAthletes(null))
        .thenReturn(List.of(athlete("qb-1", "Lamar Jackson", "QB", "ACTIVE", "33")));
    when(playerStockRepository.findByAthleteEspnId("qb-1")).thenReturn(Optional.of(existing));

    SyncAthletesResult result = syncService.syncAthletes(null);

    assertThat(result.created()).isZero();
    assertThat(result.updated()).isEqualTo(1);
    assertThat(existing.getFullName()).isEqualTo("Lamar Jackson");
    assertThat(existing.getCurrentPrice()).isEqualByComparingTo("22.50");
    // JPA dirty checking handles the persist; no explicit save() call expected
    verify(playerStockRepository, never()).save(any());
  }

  // --- skip unsupported position ---

  @Test
  void syncAthletes_unsupportedPosition_isSkipped() {
    when(ingestionApiClient.getAthletes(null))
        .thenReturn(List.of(athlete("ol-1", "Some Lineman", "OL", "ACTIVE", "1")));

    SyncAthletesResult result = syncService.syncAthletes(null);

    assertThat(result.skipped()).isEqualTo(1);
    assertThat(result.created()).isZero();
    verify(playerStockRepository, never()).save(any());
  }

  // --- position normalization (PK → K) ---

  @Test
  void syncAthletes_ingestionPkPosition_normalizedToK() {
    when(ingestionApiClient.getAthletes(null))
        .thenReturn(List.of(athlete("k-1", "Justin Tucker", "PK", "ACTIVE", "33")));
    when(playerStockRepository.findByAthleteEspnId("k-1")).thenReturn(Optional.empty());
    when(playerStockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    SyncAthletesResult result = syncService.syncAthletes(null);

    assertThat(result.created()).isEqualTo(1);
    ArgumentCaptor<PlayerStock> captor = forClass(PlayerStock.class);
    verify(playerStockRepository).save(captor.capture());
    assertThat(captor.getValue().getPosition()).isEqualTo("K");
    assertThat(captor.getValue().getCurrentPrice()).isEqualByComparingTo("5.00");
  }

  // --- position filter mapping (K → PK for ingestion) ---

  @Test
  void syncAthletes_filterByK_passesIngestionPkToClient() {
    when(ingestionApiClient.getAthletes("PK"))
        .thenReturn(List.of(athlete("k-1", "Justin Tucker", "PK", "ACTIVE", "33")));
    when(playerStockRepository.findByAthleteEspnId("k-1")).thenReturn(Optional.empty());
    when(playerStockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    syncService.syncAthletes("K");

    verify(ingestionApiClient).getAthletes("PK");
  }

  @Test
  void syncAthletes_filterByQb_passesUppercasedPositionToClient() {
    when(ingestionApiClient.getAthletes("QB")).thenReturn(List.of());

    syncService.syncAthletes("qb");

    verify(ingestionApiClient).getAthletes("QB");
  }

  // --- status mapping ---

  @Test
  void syncAthletes_inactiveAthlete_setsInactiveStatus() {
    when(ingestionApiClient.getAthletes(null))
        .thenReturn(List.of(athlete("qb-1", "Injured QB", "QB", "INACTIVE", "33")));
    when(playerStockRepository.findByAthleteEspnId("qb-1")).thenReturn(Optional.empty());
    when(playerStockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    syncService.syncAthletes(null);

    ArgumentCaptor<PlayerStock> captor = forClass(PlayerStock.class);
    verify(playerStockRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(StockStatus.INACTIVE);
  }

  @Test
  void syncAthletes_unknownStatus_defaultsToActive() {
    when(ingestionApiClient.getAthletes(null))
        .thenReturn(List.of(athlete("qb-1", "Mystery QB", "QB", "SUSPENDED", "33")));
    when(playerStockRepository.findByAthleteEspnId("qb-1")).thenReturn(Optional.empty());
    when(playerStockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    syncService.syncAthletes(null);

    ArgumentCaptor<PlayerStock> captor = forClass(PlayerStock.class);
    verify(playerStockRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(StockStatus.ACTIVE);
  }

  // --- totals ---

  @Test
  void syncAthletes_mixedAthletes_returnsCorrectCounts() {
    PlayerStock existingRb = new PlayerStock();
    existingRb.setAthleteEspnId("rb-1");
    existingRb.setFullName("Old RB");
    existingRb.setPosition("RB");
    existingRb.setCurrentPrice(new BigDecimal("14.00"));
    existingRb.setStatus(StockStatus.ACTIVE);

    when(ingestionApiClient.getAthletes(null))
        .thenReturn(
            List.of(
                athlete("qb-1", "New QB", "QB", "ACTIVE", "1"),
                athlete("rb-1", "Existing RB", "RB", "ACTIVE", "2"),
                athlete("ol-1", "Lineman", "OL", "ACTIVE", "3")));
    when(playerStockRepository.findByAthleteEspnId("qb-1")).thenReturn(Optional.empty());
    when(playerStockRepository.findByAthleteEspnId("rb-1")).thenReturn(Optional.of(existingRb));
    when(playerStockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    SyncAthletesResult result = syncService.syncAthletes(null);

    assertThat(result.created()).isEqualTo(1);
    assertThat(result.updated()).isEqualTo(1);
    assertThat(result.skipped()).isEqualTo(1);
    assertThat(result.totalFetched()).isEqualTo(3);
  }
}
