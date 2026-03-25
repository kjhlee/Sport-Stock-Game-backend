package com.sportstock.ingestion.service;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import com.sportstock.ingestion.repo.SeasonWeekRepository;
import com.sportstock.ingestion.service.IngestionOrchestrationService.SyncType;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IngestionScheduler {
  private final IngestionOrchestrationService orchestrationService;
  private final SeasonQueryService seasonQueryService;
  private final SeasonWeekRepository seasonWeekRepository;

  public IngestionScheduler(
      IngestionOrchestrationService orchestrationService,
      SeasonQueryService seasonQueryService,
      SeasonWeekRepository seasonWeekRepository) {
    this.orchestrationService = orchestrationService;
    this.seasonQueryService = seasonQueryService;
    this.seasonWeekRepository = seasonWeekRepository;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onStartupCheck() {
    int currentYear = LocalDate.now(ZoneOffset.UTC).getYear();
    boolean hasData = seasonWeekRepository.existsBySeasonYear(currentYear);
    if (!hasData) {
      log.info("No season data for year {}, triggering PRESEASON_LOAD", currentYear);
      if (orchestrationService.tryAcquire(SyncType.PRESEASON_LOAD)) {
        orchestrationService.runPreseasonLoad(currentYear);
      }
    }
  }

  @Scheduled(cron = "0 0 0 1 8 *")
  public void scheduledPreseasonLoad() {
    int year = LocalDate.now(ZoneOffset.UTC).getYear();
    log.info("Scheduled PRESEASON_LOAD for season {}", year);
    if (orchestrationService.tryAcquire(SyncType.PRESEASON_LOAD)) {
      orchestrationService.runPreseasonLoad(year);
    }
  }

  @Scheduled(fixedDelay = 60000)
  public void pollGameDay() {
    CurrentWeekResponse currentWeek;
    try {
      currentWeek = seasonQueryService.getCurrentWeek();
    } catch (Exception e) {
      return;
    }

    if (orchestrationService.tryAcquire(SyncType.GAME_REFRESH)) {
      orchestrationService.runGameRefresh(
          currentWeek.seasonYear(), Integer.parseInt(currentWeek.seasonType()), currentWeek.week());
    }
  }

  @Scheduled(cron = "0 0 6 * * *")
  public void runDailyCatchup() {
    CurrentWeekResponse currentWeek;
    try {
      currentWeek = seasonQueryService.getCurrentWeek();
    } catch (Exception e) {
      return;
    }

    log.info("Starting scheduled DAILY_CATCHUP");
    if (orchestrationService.tryAcquire(SyncType.DAILY_CATCHUP)) {
      orchestrationService.runDailyCatchup(
          currentWeek.seasonYear(), Integer.parseInt(currentWeek.seasonType()), currentWeek.week());
    }
  }

  @Scheduled(cron = "0 0 10 * * TUE")
  public void runWeeklyCatchup() {
    CurrentWeekResponse currentWeek;
    try {
      currentWeek = seasonQueryService.getCurrentWeek();
    } catch (Exception e) {
      return;
    }

    log.info("Starting scheduled WEEKLY_CATCHUP");
    if (orchestrationService.tryAcquire(SyncType.WEEKLY_CATCHUP)) {
      orchestrationService.runWeeklyCatchup(
          currentWeek.seasonYear(), Integer.parseInt(currentWeek.seasonType()), currentWeek.week());
    }
  }
}
