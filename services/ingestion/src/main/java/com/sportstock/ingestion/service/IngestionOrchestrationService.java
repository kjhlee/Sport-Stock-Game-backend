package com.sportstock.ingestion.service;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class IngestionOrchestrationService {

  public enum SyncType {
    PRESEASON_LOAD,
    GAME_REFRESH,
    DAILY_CATCHUP,
    WEEKLY_CATCHUP,
    ADMIN_SYNC
  }

  private static final Set<SyncType> EXCLUSIVE_TYPES =
      EnumSet.of(SyncType.ADMIN_SYNC, SyncType.PRESEASON_LOAD);

  private final TeamIngestionService teamIngestionService;
  private final RosterIngestionService rosterIngestionService;
  private final EventIngestionService eventIngestionService;
  private final EventSummaryIngestionService eventSummaryIngestionService;
  private final SeasonIngestionService seasonIngestionService;
  private final SeasonQueryService seasonQueryService;
  private final TransactionTemplate transactionTemplate;

  private final Object orchestrationLock = new Object();
  private final Set<SyncType> activeSyncs = EnumSet.noneOf(SyncType.class);

  public IngestionOrchestrationService(
      TeamIngestionService teamIngestionService,
      RosterIngestionService rosterIngestionService,
      EventIngestionService eventIngestionService,
      EventSummaryIngestionService eventSummaryIngestionService,
      SeasonIngestionService seasonIngestionService,
      SeasonQueryService seasonQueryService,
      TransactionTemplate transactionTemplate) {
    this.teamIngestionService = teamIngestionService;
    this.rosterIngestionService = rosterIngestionService;
    this.eventIngestionService = eventIngestionService;
    this.eventSummaryIngestionService = eventSummaryIngestionService;
    this.seasonIngestionService = seasonIngestionService;
    this.seasonQueryService = seasonQueryService;
    this.transactionTemplate = transactionTemplate;
  }

  public boolean tryAcquire(SyncType syncType) {
    synchronized (orchestrationLock) {
      for (SyncType active : activeSyncs) {
        if (EXCLUSIVE_TYPES.contains(active)) {
          log.warn("Skipping sync type {} because active sync is exclusive", syncType);
          return false;
        }
      }
      if (EXCLUSIVE_TYPES.contains(syncType) && !activeSyncs.isEmpty()) {
        log.warn(
            "Skipping sync type {} because it is exclusive and another sync is active", syncType);
        return false;
      }
      if (syncType == SyncType.DAILY_CATCHUP && activeSyncs.contains(SyncType.WEEKLY_CATCHUP)) {
        log.warn("Skipping sync type {} because it conflicts with weekly catchup", syncType);
        return false;
      }
      if (activeSyncs.contains(syncType)) {
        log.warn("Skipping sync type {} because it conflicts with active sync", syncType);
        return false;
      }

      activeSyncs.add(syncType);
      return true;
    }
  }

  public void release(SyncType syncType) {
    synchronized (orchestrationLock) {
      activeSyncs.remove(syncType);
    }
  }

  @Async("ingestionExecutor")
  public void runPreseasonLoad(int seasonYear) {
    try {
      log.info("Starting PRESEASON_LOAD for season {}", seasonYear);

      teamIngestionService.ingestTeams();

      var teams = teamIngestionService.listTeams();
      for (var team : teams) {
        try {
          transactionTemplate.executeWithoutResult(
              status -> teamIngestionService.ingestTeamDetail(team.espnId(), seasonYear));
        } catch (Exception e) {
          log.error("Failed to ingest team detail for {}: {}", team.espnId(), e.getMessage());
        }
      }

      eventIngestionService.ingestScoreboard(seasonYear, 2, 1);
      rosterIngestionService.ingestAllRosters(seasonYear, null, null);
      verifyPriorSeasonData(seasonYear - 1);

      log.info("PRESEASON_LOAD complete for season {}", seasonYear);
    } catch (Exception e) {
      log.error("PRESEASON_LOAD failed for season {}: {}", seasonYear, e.getMessage());
    } finally {
      release(SyncType.PRESEASON_LOAD);
    }
  }

  @Async("ingestionExecutor")
  public void runGameRefresh(int seasonYear, int seasonType, int week) {
    try {
      LocalDate today = LocalDate.now(ZoneOffset.UTC);
      Instant rangeStart = today.minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
      Instant rangeEnd = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

      boolean hasIncompleteEvents = eventIngestionService.hasIncompleteEvents(rangeStart, rangeEnd);
      List<String> needingSummary =
          eventIngestionService.getCompletedEventsNeedingSummary(rangeStart, rangeEnd);

      if (!hasIncompleteEvents && needingSummary.isEmpty()) {
        return;
      }

      if (hasIncompleteEvents) {
        try {
          eventIngestionService.ingestScoreboard(seasonYear, seasonType, week);
        } catch (Exception e) {
          log.error("GAME_REFRESH: failed to ingest scoreboard: {}", e.getMessage());
          return;
        }
        needingSummary =
            eventIngestionService.getCompletedEventsNeedingSummary(rangeStart, rangeEnd);
      }

      for (String eventEspnId : needingSummary) {
        enqueueSummary(eventEspnId);
      }

      if (!needingSummary.isEmpty()) {
        log.debug("GAME_REFRESH: {} summaries processed", needingSummary.size());
      }
    } finally {
      release(SyncType.GAME_REFRESH);
    }
  }

  @Async("ingestionExecutor")
  public void runDailyCatchup(int seasonYear, int seasonType, int week) {
    try {
      log.info("Starting daily catchup for season {}, week {}", seasonYear, week);

      Instant now = Instant.now();
      Instant rangeStart = now.minusSeconds(36 * 3600); // 36 hrs

      List<String> eventsNeedingSummary =
          eventIngestionService.getCompletedEventsNeedingSummary(rangeStart, now);
      int summaryCount = 0;

      for (String eventEspnId : eventsNeedingSummary) {
        if (enqueueSummary(eventEspnId)) summaryCount++;
      }
      Instant todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
      rosterIngestionService.ingestRostersNeedingSync(seasonYear, todayStart);

      log.info("DAILY_CATCHUP complete: {} summaries ingested, roster sync done", summaryCount);
    } catch (Exception e) {
      log.error("DAILY_CATCHUP failed: {}", e.getMessage());
    } finally {
      release(SyncType.DAILY_CATCHUP);
    }
  }

  @Async("ingestionExecutor")
  public void runWeeklyCatchup(int seasonYear, int seasonType, int week) {
    try {
      log.info("Starting WEEKLY_CATCHUP");
      Instant now = Instant.now();
      Instant rangeStart = now.minusSeconds(8 * 24 * 3600); // 8 days ago

      List<String> needingSummary =
          eventIngestionService.getCompletedEventsNeedingSummary(rangeStart, now);
      int summaryCount = 0;
      for (String eventEspnId : needingSummary) {
        if (enqueueSummary(eventEspnId)) summaryCount++;
      }

      rosterIngestionService.ingestAllRosters(seasonYear, null, null);

      Optional<CurrentWeekResponse> nextWeek = seasonQueryService.getNextWeek();
      if (nextWeek.isPresent()) {
        CurrentWeekResponse nw = nextWeek.get();
        try {
          eventIngestionService.ingestScoreboard(
              nw.seasonYear(), Integer.parseInt(nw.seasonType()), nw.week());
          log.info(
              "WEEKLY_CATCHUP: pre-ingested schedule for {} {} Week {}",
              nw.seasonYear(),
              nw.seasonTypeName(),
              nw.week());
        } catch (Exception e) {
          log.warn("WEEKLY_CATCHUP: failed to pre-ingest next week schedule: {}", e.getMessage());
        }
      } else {
        log.info("WEEKLY_CATCHUP: no next week found — may be end of season");
      }

      log.info(
          "WEEKLY_CATCHUP complete: {} summaries ingested, full roster sync done", summaryCount);
    } catch (Exception e) {
      log.error("WEEKLY_CATCHUP failed: {}", e.getMessage());
    } finally {
      release(SyncType.WEEKLY_CATCHUP);
    }
  }

  @Async("ingestionExecutor")
  public void runAdminSync(
      int seasonYear, int seasonType, int week, boolean force, List<String> teamEspnIds) {
    try {
      log.info(
          "Starting ADMIN_SYNC for season {} type {} week {} (force={})",
          seasonYear,
          seasonType,
          week,
          force);

      teamIngestionService.ingestTeams();
      eventIngestionService.ingestScoreboard(seasonYear, seasonType, week);

      var teams =
          (teamEspnIds != null && !teamEspnIds.isEmpty())
              ? teamEspnIds.stream().map(teamIngestionService::getTeamByEspnId).toList()
              : teamIngestionService.listTeams();

      int teamSuccess = 0;
      int teamFailed = 0;
      List<String> teamFailedIds = new ArrayList<>();
      for (var team : teams) {
        try {
          transactionTemplate.executeWithoutResult(
              status -> teamIngestionService.ingestTeamDetail(team.espnId(), seasonYear));
          teamSuccess++;
        } catch (Exception e) {
          teamFailed++;
          teamFailedIds.add(team.espnId());
          log.error("Failed to ingest team detail for {}: {}", team.espnId(), e.getMessage());
        }
      }
      log.info(
          "Ingested {} team details ({} failed{})",
          teamSuccess,
          teamFailed,
          teamFailedIds.isEmpty() ? "" : ", IDs: " + teamFailedIds);

      rosterIngestionService.ingestAllRosters(seasonYear, null, teamEspnIds);

      var events = eventIngestionService.listEvents(seasonYear, seasonType, week);
      int eventSuccess = 0;
      int eventFailed = 0;
      List<String> eventFailedIds = new ArrayList<>();
      for (var event : events) {
        if (!force && event.summaryIngestedAt() != null) continue;
        try {
          transactionTemplate.executeWithoutResult(
              status -> eventSummaryIngestionService.ingestEventSummary(event.espnId()));
          eventSuccess++;
        } catch (Exception e) {
          eventFailed++;
          eventFailedIds.add(event.espnId());
          log.error("Failed to ingest event summary for {}: {}", event.espnId(), e.getMessage());
        }
      }
      log.info(
          "Ingested {} event summaries ({} failed{})",
          eventSuccess,
          eventFailed,
          eventFailedIds.isEmpty() ? "" : ", IDs: " + eventFailedIds);

      log.info("ADMIN_SYNC complete");
    } finally {
      release(SyncType.ADMIN_SYNC);
    }
  }

  public boolean enqueueSummary(String eventEspnId) {
    try {
      transactionTemplate.executeWithoutResult(
          status -> eventSummaryIngestionService.ingestEventSummary(eventEspnId));
      return true;
    } catch (Exception e) {
      log.error("Failed to ingest summary for event {}: {}", eventEspnId, e.getMessage());
      return false;
    }
  }

  private void verifyPriorSeasonData(int priorSeasonYear) {
    var events = eventIngestionService.listEvents(priorSeasonYear, null, null);
    long missingCount = events.stream().filter(e -> e.summaryIngestedAt() == null).count();
    if (missingCount > 0) {
      log.warn(
          "Prior season {} has {} events with missing summaries — "
              + "run ADMIN_SYNC with force=true to backfill",
          priorSeasonYear,
          missingCount);
    }
  }
}
