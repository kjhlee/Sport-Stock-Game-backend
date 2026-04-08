package com.sportstock.stockmarket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportstock.common.dto.stock_market.IngestionInjuryStatusDto;
import com.sportstock.common.enums.stock_market.StockType;
import com.sportstock.stockmarket.client.IngestionApiClient;
import com.sportstock.stockmarket.repository.StockRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockLockService Unit Tests")
class StockLockServiceTest {

  @Mock private StockRepository stockRepository;
  @Mock private IngestionApiClient ingestionApiClient;

  private StockLockService stockLockService;

  @BeforeEach
  void setUp() {
    stockLockService = new StockLockService(stockRepository, ingestionApiClient);
  }

  @Test
  @DisplayName("Should lock team defenses and players separately for an event")
  void shouldLockTeamDefensesAndPlayersSeparatelyForAnEvent() {
    when(ingestionApiClient.getEventTeamEspnIds("401000001")).thenReturn(List.of("5", "12"));
    when(ingestionApiClient.getEventRosterEspnIds("401000001", "5")).thenReturn(List.of("101", "102"));
    when(ingestionApiClient.getEventRosterEspnIds("401000001", "12")).thenReturn(List.of("201"));
    when(stockRepository.lockByEspnIdsAndType(List.of("5", "12"), StockType.TEAM_DEFENSE))
        .thenReturn(2);
    when(stockRepository.lockByEspnIdsAndType(List.of("101", "102", "201"), StockType.PLAYER))
        .thenReturn(3);

    int locked = stockLockService.lockPlayersForEvent("401000001");

    assertThat(locked).isEqualTo(5);
    verify(stockRepository).lockByEspnIdsAndType(List.of("5", "12"), StockType.TEAM_DEFENSE);
    verify(stockRepository)
        .lockByEspnIdsAndType(List.of("101", "102", "201"), StockType.PLAYER);
  }

  @Test
  @DisplayName("Should sync injury locks for players only")
  void shouldSyncInjuryLocksForPlayersOnly() {
    when(ingestionApiClient.getInjuredAthletes(2026))
        .thenReturn(
            List.of(
                new IngestionInjuryStatusDto("101", "5", "Questionable", "ACTIVE"),
                new IngestionInjuryStatusDto("101", "5", "Questionable", "ACTIVE"),
                new IngestionInjuryStatusDto("202", "12", "Out", "ACTIVE")));
    when(stockRepository.clearAllPlayerInjuryLocks()).thenReturn(4);
    when(stockRepository.setInjuryLockedByEspnIds(List.of("101", "202"))).thenReturn(2);

    StockLockService.InjurySyncResult result = stockLockService.syncInjuryStatuses(2026);

    assertThat(result.locked()).isEqualTo(2);
    assertThat(result.unlocked()).isEqualTo(4);
    verify(stockRepository).clearAllPlayerInjuryLocks();
    verify(stockRepository).setInjuryLockedByEspnIds(List.of("101", "202"));
  }

  @Test
  @DisplayName("Should only clear existing player injury locks when no injuries are returned")
  void shouldOnlyClearExistingPlayerInjuryLocksWhenNoInjuriesAreReturned() {
    when(ingestionApiClient.getInjuredAthletes(2026)).thenReturn(List.of());
    when(stockRepository.clearAllPlayerInjuryLocks()).thenReturn(3);

    StockLockService.InjurySyncResult result = stockLockService.syncInjuryStatuses(2026);

    assertThat(result.locked()).isEqualTo(0);
    assertThat(result.unlocked()).isEqualTo(3);
    verify(stockRepository).clearAllPlayerInjuryLocks();
    verify(stockRepository, never()).setInjuryLockedByEspnIds(List.of());
  }
}
