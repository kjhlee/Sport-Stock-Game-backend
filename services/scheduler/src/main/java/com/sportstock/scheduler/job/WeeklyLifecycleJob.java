package com.sportstock.scheduler.job;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import com.sportstock.common.dto.ingestion.EventResponse;
import com.sportstock.common.dto.league.LeagueResponse;
import com.sportstock.common.dto.stock_market.PriceUpdateResponse;
import com.sportstock.scheduler.client.IngestionClient;
import com.sportstock.scheduler.client.LeagueClient;
import com.sportstock.scheduler.client.StockMarketClient;
import com.sportstock.scheduler.client.TransactionClient;
import com.sportstock.scheduler.config.ProjectionSyncLock;
import com.sportstock.scheduler.entity.EventState;
import com.sportstock.scheduler.repo.EventStateRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyLifecycleJob {

  private static final int STALE_ROSTER_HOURS = 6;
  private static final String STATE_FINALIZED = "FINALIZED";
  private static final Set<String> COMPLETED_STATES = Set.of("post");

  private final IngestionClient ingestionClient;
  private final LeagueClient leagueClient;
  private final TransactionClient transactionClient;
  private final StockMarketClient stockMarketClient;
  private final EventStateRepository eventStateRepository;
  private final GameDayPollingJob gameDayPollingJob;
  private final ProjectionSyncLock projectionSyncLock;

  @Scheduled(cron = "${scheduler.weekly-lifecycle.cron}")
  public void run() {
    if (!projectionSyncLock.tryAcquire()) {
      log.warn(
          "WeeklyLifecycleJob skipped because another projection sync chain is already running");
      return;
    }

    log.info("WeeklyLifecycleJob started");

    try {
      if (!ingestionClient.isSeasonActive()) {
        log.info("Season not active, skipping weekly lifecycle");
        return;
      }

      CurrentWeekResponse currentWeek = ingestionClient.getCurrentWeek();
      int currentWeekNum = currentWeek.week();
      int seasonYear = currentWeek.seasonYear();
      int seasonType = Integer.parseInt(currentWeek.seasonType());

      log.info(
          "Running weekly lifecycle: season {}/{}, currentWeek={}",
          seasonYear,
          seasonType,
          currentWeekNum);

      CurrentWeekResponse priorWeek = ingestionClient.getPriorWeek();

      if (priorWeek != null) {
        int priorSeasonYear = priorWeek.seasonYear();
        int priorSeasonType = Integer.parseInt(priorWeek.seasonType());
        int priorWeekNum = priorWeek.week();

        log.info(
            "Prior week: season {}/{} week {}", priorSeasonYear, priorSeasonType, priorWeekNum);
        forceFinalizePriorWeek(priorSeasonYear, priorSeasonType, priorWeekNum);
      } else {
        log.info("No prior week (first week of season), skipping force-finalize and liquidation");
      }

      List<LeagueResponse> activeLeagues = leagueClient.getActiveLeagues();
      log.info("Processing {} active leagues", activeLeagues.size());

      for (LeagueResponse league : activeLeagues) {
        processLeague(league, currentWeekNum, currentWeek.seasonType(), priorWeek);
      }

      try {
        ingestionClient.syncScoreboard(seasonYear, seasonType, currentWeekNum);
        log.info("Seeded events for season {}/{} week {}", seasonYear, seasonType, currentWeekNum);
      } catch (Exception e) {
        log.error("Failed to seed events for current week: {}", e.getMessage());
      }

      int unlocked = stockMarketClient.unlockAll();
      log.info("Unlocked {} stocks", unlocked);

      boolean canDelist = runProjectionSync(seasonYear, seasonType, currentWeekNum);

      if (canDelist) {
        try {
          int delisted =
              stockMarketClient.delistUnprojectedStocks(seasonYear, seasonType, currentWeekNum);
          log.info("Delisted {} unprojected stocks", delisted);
        } catch (Exception e) {
          log.error("Failed to delist unprojected stocks: {}", e.getMessage());
        }
      } else {
        log.warn("Skipping delist step because projection sync failed or updated 0 prices");
      }

      if (priorWeek != null) {
        clearPriorWeekEventStates(
            priorWeek.week(), priorWeek.seasonYear(), Integer.parseInt(priorWeek.seasonType()));
      }

      log.info("WeeklyLifecycleJob completed");
    } catch (Exception e) {
      log.warn(
          "Skipping weekly lifecycle because dependencies are unavailable: {}", e.getMessage());
    } finally {
      projectionSyncLock.release();
    }
  }

  private void forceFinalizePriorWeek(int priorSeasonYear, int priorSeasonType, int priorWeekNum) {
    try {
      ingestionClient.syncScoreboard(priorSeasonYear, priorSeasonType, priorWeekNum);
    } catch (Exception e) {
      log.warn("Failed to sync prior week scoreboard: {}", e.getMessage());
    }

    List<EventResponse> priorEvents;
    try {
      priorEvents = ingestionClient.getEvents(priorSeasonYear, priorSeasonType, priorWeekNum);
    } catch (Exception e) {
      log.warn("Failed to fetch prior week events: {}", e.getMessage());
      return;
    }

    Set<String> finalizedEspnIds =
        eventStateRepository
            .findByWeekNumberAndSeasonYearAndSeasonType(
                priorWeekNum, priorSeasonYear, priorSeasonType)
            .stream()
            .filter(es -> STATE_FINALIZED.equals(es.getStatus()))
            .map(EventState::getEventEspnId)
            .collect(Collectors.toSet());

    int finalized = 0;
    int skippedLive = 0;
    for (EventResponse event : priorEvents) {
      if (finalizedEspnIds.contains(event.espnId())) {
        continue;
      }

      if (Boolean.TRUE.equals(event.statusCompleted())
          || COMPLETED_STATES.contains(
              event.statusState() != null ? event.statusState().toLowerCase() : "")) {
        try {
          gameDayPollingJob.closeEvent(event.espnId());
          finalized++;
          log.info("Force-finalized straggler event {}", event.espnId());
        } catch (Exception e) {
          log.error("Failed to force-finalize event {}: {}", event.espnId(), e.getMessage());
        }
      } else {
        skippedLive++;
        log.warn(
            "Skipping unfinished event {} (status={}): game may be delayed/postponed",
            event.espnId(),
            event.statusState());
      }
    }

    log.info(
        "Force-finalize complete: {} finalized, {} skipped (still live/delayed)",
        finalized,
        skippedLive);
  }

  private void processLeague(
      LeagueResponse league,
      int currentWeekNum,
      String currentSeasonType,
      CurrentWeekResponse priorWeek) {
    Long leagueId = league.id();

    if (priorWeek != null) {
      try {
        transactionClient.liquidateAssets(
            leagueId, priorWeek.week(), priorWeek.seasonType());
        log.info("Liquidated assets for league {} week {}", leagueId, priorWeek.week());
      } catch (Exception e) {
        log.warn(
            "Failed to liquidate assets for league {} week {}: {}",
            leagueId,
            priorWeek.week(),
            e.getMessage());
      }
    }

    try {
      transactionClient.issueWeeklyStipends(leagueId, league.weeklyStipendAmount(), currentWeekNum);
      log.info("Issued weekly stipend for league {} week {}", leagueId, currentWeekNum);
    } catch (Exception e) {
      log.error(
          "Failed to issue weekly stipend for league {} week {}: {}",
          leagueId,
          currentWeekNum,
          e.getMessage());
    }

    try {
      transactionClient.initializePortfolioHistory(leagueId, currentWeekNum, currentSeasonType);
      log.info("Initialized portfolio history for league {} week {}", leagueId, currentWeekNum);
    } catch (Exception e) {
      log.error(
          "Failed to initialize portfolio history for league {} week {}: {}",
          leagueId,
          currentWeekNum,
          e.getMessage());
    }
  }

  private boolean runProjectionSync(int seasonYear, int seasonType, int weekNumber) {
    try {
      ingestionClient.syncStaleRosters(seasonYear, STALE_ROSTER_HOURS);
      log.info("Refreshed stale rosters");
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
      log.info("Synced projections for week {}", weekNumber);
    } catch (Exception e) {
      log.error("Failed to sync projections: {}", e.getMessage());
    }

    if (!projectionsSynced) {
      log.warn("Projection sync failed, skipping relist and price update");
      return false;
    }

    try {
      int relisted = stockMarketClient.relistProjectedStocks(seasonYear, seasonType, weekNumber);
      log.info("Relisted {} stocks with new projections", relisted);
    } catch (Exception e) {
      log.error("Failed to relist projected stocks: {}", e.getMessage());
    }

    int pricesUpdated = 0;
    try {
      PriceUpdateResponse priceResult =
          stockMarketClient.updateProjectedPrices(seasonYear, seasonType, weekNumber);
      pricesUpdated = priceResult != null ? priceResult.updated() : 0;
      log.info("Updated {} projected prices", pricesUpdated);
    } catch (Exception e) {
      log.error("Failed to update projected prices: {}", e.getMessage());
    }

    if (pricesUpdated == 0) {
      log.warn("Projected prices updated 0 stocks, delist will be skipped");
      return false;
    }

    return true;
  }

  private void clearPriorWeekEventStates(int priorWeekNum, int seasonYear, int seasonType) {
    var priorStates =
        eventStateRepository.findByWeekNumberAndSeasonYearAndSeasonType(
            priorWeekNum, seasonYear, seasonType);
    if (!priorStates.isEmpty()) {
      eventStateRepository.deleteAll(priorStates);
      log.info("Cleared {} event_state rows for prior week {}", priorStates.size(), priorWeekNum);
    }
  }
}
