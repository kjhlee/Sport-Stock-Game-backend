package com.sportstock.scheduler.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import com.sportstock.common.dto.ingestion.EventResponse;
import com.sportstock.common.dto.league.LeagueResponse;
import com.sportstock.common.dto.stock_market.PriceUpdateResponse;
import com.sportstock.scheduler.client.IngestionClient;
import com.sportstock.scheduler.client.LeagueClient;
import com.sportstock.scheduler.client.StockMarketClient;
import com.sportstock.scheduler.client.TransactionClient;
import com.sportstock.scheduler.entity.EventState;
import com.sportstock.scheduler.repo.EventStateRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeeklyLifecycleJobTest {

  @Mock private IngestionClient ingestionClient;
  @Mock private LeagueClient leagueClient;
  @Mock private TransactionClient transactionClient;
  @Mock private StockMarketClient stockMarketClient;
  @Mock private EventStateRepository eventStateRepository;
  @Mock private GameDayPollingJob gameDayPollingJob;

  private WeeklyLifecycleJob job;

  @BeforeEach
  void setUp() {
    job =
        new WeeklyLifecycleJob(
            ingestionClient,
            leagueClient,
            transactionClient,
            stockMarketClient,
            eventStateRepository,
            gameDayPollingJob);
  }

  @Test
  void run_forceFinalizesCompletedPriorWeekAndDelistsAfterSuccessfulProjectionSync() {
    CurrentWeekResponse currentWeek = week(2026, "3", 1);
    CurrentWeekResponse priorWeek = week(2026, "2", 18);
    LeagueResponse league = league(42L);

    EventResponse completedEvent =
        new EventResponse(
            "evt-complete",
            null,
            null,
            Instant.now(),
            2026,
            2,
            null,
            18,
            null,
            null,
            null,
            null,
            "post",
            true,
            null,
            null,
            null,
            null,
            null,
            null);
    EventResponse unfinishedEvent =
        new EventResponse(
            "evt-live",
            null,
            null,
            Instant.now(),
            2026,
            2,
            null,
            18,
            null,
            null,
            null,
            null,
            "in",
            false,
            null,
            null,
            null,
            null,
            null,
            null);

    EventState cleanupState = new EventState();
    cleanupState.setEventEspnId("evt-cleanup");
    cleanupState.setStatus("FINALIZED");
    cleanupState.setWeekNumber(18);
    cleanupState.setSeasonYear(2026);
    cleanupState.setSeasonType(2);

    when(ingestionClient.isSeasonActive()).thenReturn(true);
    when(ingestionClient.getCurrentWeek()).thenReturn(currentWeek);
    when(ingestionClient.getPriorWeek()).thenReturn(priorWeek);
    when(ingestionClient.getEvents(2026, 2, 18))
        .thenReturn(List.of(completedEvent, unfinishedEvent));
    when(leagueClient.getActiveLeagues()).thenReturn(List.of(league));
    when(stockMarketClient.unlockAll()).thenReturn(8);
    when(stockMarketClient.relistProjectedStocks(2026, 3, 1)).thenReturn(2);
    when(stockMarketClient.updateProjectedPrices(2026, 3, 1))
        .thenReturn(new PriceUpdateResponse(14, 3));
    when(stockMarketClient.delistUnprojectedStocks(2026, 3, 1)).thenReturn(5);
    when(eventStateRepository.findByWeekNumberAndSeasonYearAndSeasonType(18, 2026, 2))
        .thenReturn(List.of(), List.of(cleanupState));

    job.run();

    verify(ingestionClient).syncScoreboard(2026, 2, 18);
    verify(gameDayPollingJob).closeEvent("evt-complete");
    verify(gameDayPollingJob, never()).closeEvent("evt-live");
    verify(transactionClient).liquidateAssets(42L, 18);
    verify(transactionClient).issueWeeklyStipends(42L, new BigDecimal("25.00"), 1);
    verify(ingestionClient).syncScoreboard(2026, 3, 1);
    verify(stockMarketClient).unlockAll();
    verify(ingestionClient).syncStaleRosters(2026, 6);
    verify(stockMarketClient).syncAthletes(null);
    verify(stockMarketClient).syncInjuries(2026);
    verify(ingestionClient).syncProjections(2026, 3, 1);
    verify(stockMarketClient).relistProjectedStocks(2026, 3, 1);
    verify(stockMarketClient).updateProjectedPrices(2026, 3, 1);
    verify(stockMarketClient).delistUnprojectedStocks(2026, 3, 1);
    verify(eventStateRepository).deleteAll(List.of(cleanupState));

    InOrder inOrder =
        inOrder(ingestionClient, stockMarketClient, transactionClient, eventStateRepository);
    inOrder.verify(ingestionClient).syncProjections(2026, 3, 1);
    inOrder.verify(stockMarketClient).relistProjectedStocks(2026, 3, 1);
    inOrder.verify(stockMarketClient).updateProjectedPrices(2026, 3, 1);
    inOrder.verify(stockMarketClient).delistUnprojectedStocks(2026, 3, 1);
  }

  @Test
  void run_skipsPriorWeekActionsAndDelistWhenProjectedPricesUpdateZeroStocks() {
    CurrentWeekResponse currentWeek = week(2026, "2", 6);
    LeagueResponse league = league(7L);

    when(ingestionClient.isSeasonActive()).thenReturn(true);
    when(ingestionClient.getCurrentWeek()).thenReturn(currentWeek);
    when(ingestionClient.getPriorWeek()).thenReturn(null);
    when(leagueClient.getActiveLeagues()).thenReturn(List.of(league));
    when(stockMarketClient.unlockAll()).thenReturn(3);
    when(stockMarketClient.relistProjectedStocks(2026, 2, 6)).thenReturn(1);
    when(stockMarketClient.updateProjectedPrices(2026, 2, 6))
        .thenReturn(new PriceUpdateResponse(0, 25));

    job.run();

    verify(gameDayPollingJob, never()).closeEvent(any());
    verify(transactionClient, never()).liquidateAssets(any(), any(int.class));
    verify(transactionClient).issueWeeklyStipends(7L, new BigDecimal("25.00"), 6);
    verify(ingestionClient).syncScoreboard(2026, 2, 6);
    verify(stockMarketClient).relistProjectedStocks(2026, 2, 6);
    verify(stockMarketClient).updateProjectedPrices(2026, 2, 6);
    verify(stockMarketClient, never())
        .delistUnprojectedStocks(any(int.class), any(int.class), any(int.class));
    verify(eventStateRepository, never()).deleteAll(any());
  }

  private static CurrentWeekResponse week(int seasonYear, String seasonType, int week) {
    return new CurrentWeekResponse(
        seasonYear,
        seasonType,
        "Season Type",
        week,
        "Week " + week,
        Instant.parse("2026-01-01T00:00:00Z"),
        Instant.parse("2026-01-08T00:00:00Z"));
  }

  private static LeagueResponse league(Long id) {
    return new LeagueResponse(
        id,
        100L,
        "League",
        "ACTIVE",
        12,
        OffsetDateTime.parse("2026-09-01T00:00:00Z"),
        OffsetDateTime.parse("2027-01-15T00:00:00Z"),
        new BigDecimal("100.00"),
        new BigDecimal("25.00"),
        OffsetDateTime.parse("2026-09-01T00:00:00Z"),
        OffsetDateTime.parse("2026-08-01T00:00:00Z"),
        8);
  }
}
