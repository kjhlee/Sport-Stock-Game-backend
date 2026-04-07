package com.sportstock.scheduler.job;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.sportstock.scheduler.client.IngestionClient;
import com.sportstock.scheduler.client.StockMarketClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectionSyncJob {

    private static final int STALE_ROSTER_HOURS = 6;

    private final IngestionClient ingestionClient;
    private final StockMarketClient stockMarketClient;

    @Scheduled(cron = "${scheduler.projection-sync.cron}")
    public void run() {
        log.info("ProjectionSyncJob started");

        try {
            if (!ingestionClient.isSeasonActive()) {
                log.info("Season not active, skipping projection sync");
                return;
            }

            CurrentWeekResponse week = ingestionClient.getCurrentWeek();
            int seasonYear = week.seasonYear();
            int seasonType = Integer.parseInt(week.seasonType());
            int weekNumber = week.week();

            log.info("Syncing projections for {}/{} week {}", seasonYear, seasonType, weekNumber);

            try {
                ingestionClient.syncStaleRosters(seasonYear, STALE_ROSTER_HOURS);
                log.info("Refreshed stale rosters before injury sync");
            } catch (Exception e) {
                log.error("Failed to refresh rosters: {}", e.getMessage());
            }

            try {
                stockMarketClient.syncAthletes(null);
                log.info("Synced athlete universe");
            } catch (Exception e) {
                log.error("Failed to sync athletes: {}", e.getMessage());
            }

            try {
                stockMarketClient.syncInjuries(seasonYear);
                log.info("Synced injuries");
            } catch (Exception e) {
                log.error("Failed to sync injuries: {}", e.getMessage());
            }

            boolean projectionsSynced = false;
            try {
                ingestionClient.syncProjections(seasonYear, seasonType, weekNumber);
                projectionsSynced = true;
            } catch (Exception e) {
                log.error("Failed to sync projections: {}", e.getMessage());
            }

            if (projectionsSynced) {
                try {
                    stockMarketClient.relistProjectedStocks(seasonYear, seasonType, weekNumber);
                    log.info("Relisted projected stocks");
                } catch (Exception e) {
                    log.error("Failed to relist projected stocks: {}", e.getMessage());
                }

                try {
                    stockMarketClient.updateProjectedPrices(seasonYear, seasonType, weekNumber);
                    log.info("Updated projected prices");
                } catch (Exception e) {
                    log.error("Failed to update projected prices: {}", e.getMessage());
                }
            } else {
                log.warn("Skipping projected price update because projection sync failed");
            }

            log.info("ProjectionSyncJob completed");
        } catch (Exception e) {
            log.warn("Skipping projection sync because dependencies are unavailable: {}", e.getMessage());
        }
    }
}
