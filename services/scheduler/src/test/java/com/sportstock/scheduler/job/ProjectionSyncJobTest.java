package com.sportstock.scheduler.job;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import com.sportstock.scheduler.client.IngestionClient;
import com.sportstock.scheduler.client.StockMarketClient;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectionSyncJobTest {

  @Mock private IngestionClient ingestionClient;
  @Mock private StockMarketClient stockMarketClient;

  private ProjectionSyncJob job;

  @BeforeEach
  void setUp() {
    job = new ProjectionSyncJob(ingestionClient, stockMarketClient);
  }

  @Test
  void run_relistsBeforeUpdatingProjectedPricesWhenProjectionSyncSucceeds() {
    when(ingestionClient.isSeasonActive()).thenReturn(true);
    when(ingestionClient.getCurrentWeek()).thenReturn(week(2026, "2", 6));

    job.run();

    verify(ingestionClient).syncStaleRosters(2026, 6);
    verify(stockMarketClient).syncAthletes(null);
    verify(stockMarketClient).syncInjuries(2026);
    verify(ingestionClient).syncProjections(2026, 2, 6);
    verify(stockMarketClient).relistProjectedStocks(2026, 2, 6);
    verify(stockMarketClient).updateProjectedPrices(2026, 2, 6);

    InOrder inOrder = inOrder(ingestionClient, stockMarketClient);
    inOrder.verify(ingestionClient).syncProjections(2026, 2, 6);
    inOrder.verify(stockMarketClient).relistProjectedStocks(2026, 2, 6);
    inOrder.verify(stockMarketClient).updateProjectedPrices(2026, 2, 6);
  }

  @Test
  void run_skipsRelistAndPriceUpdateWhenProjectionSyncFails() {
    when(ingestionClient.isSeasonActive()).thenReturn(true);
    when(ingestionClient.getCurrentWeek()).thenReturn(week(2026, "3", 1));
    org.mockito.Mockito.doThrow(new RuntimeException("projection outage"))
        .when(ingestionClient)
        .syncProjections(2026, 3, 1);

    job.run();

    verify(ingestionClient).syncProjections(2026, 3, 1);
    verify(stockMarketClient, never()).relistProjectedStocks(2026, 3, 1);
    verify(stockMarketClient, never()).updateProjectedPrices(2026, 3, 1);
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
}
