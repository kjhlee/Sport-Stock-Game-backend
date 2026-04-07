package com.sportstock.scheduler.controller;

import com.sportstock.scheduler.job.DailyCatchupJob;
import com.sportstock.scheduler.job.GameDayPollingJob;
import com.sportstock.scheduler.job.InitialStipendCheckJob;
import com.sportstock.scheduler.job.PreseasonBootstrapJob;
import com.sportstock.scheduler.job.ProjectionSyncJob;
import com.sportstock.scheduler.job.WeeklyLifecycleJob;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scheduler")
@RequiredArgsConstructor
public class SchedulerAdminController {

  private final WeeklyLifecycleJob weeklyLifecycleJob;
  private final ProjectionSyncJob projectionSyncJob;
  private final GameDayPollingJob gameDayPollingJob;
  private final DailyCatchupJob dailyCatchupJob;
  private final InitialStipendCheckJob initialStipendCheckJob;
  private final PreseasonBootstrapJob preseasonBootstrapJob;

  @PostMapping("/trigger/weekly-lifecycle")
  public String triggerWeeklyLifecycle() {
    weeklyLifecycleJob.run();
    return "triggered";
  }

  @PostMapping("/trigger/projection-sync")
  public String triggerProjectionSync() {
    projectionSyncJob.run();
    return "triggered";
  }

  @PostMapping("/trigger/game-poll")
  public String triggerGamePoll() {
    gameDayPollingJob.run();
    return "triggered";
  }

  @PostMapping("/trigger/close-event")
  public GameDayPollingJob.CloseEventResult triggerCloseEvent(@RequestParam String eventEspnId) {
    return gameDayPollingJob.closeEvent(eventEspnId);
  }

  @PostMapping("/trigger/daily-catchup")
  public String triggerDailyCatchup() {
    dailyCatchupJob.run();
    return "triggered";
  }

  @PostMapping("/trigger/initial-stipend-check")
  public String triggerInitialStipendCheck() {
    initialStipendCheckJob.run();
    return "triggered";
  }

  @PostMapping("/trigger/preseason-bootstrap")
  public String triggerPreseasonBootstrap(@RequestParam(required = false) Integer seasonYear) {
    preseasonBootstrapJob.run(seasonYear);
    return "triggered";
  }

  @PostMapping("/test/close-event")
  public GameDayPollingJob.CloseEventResult testCloseEvent(@RequestParam String eventEspnId) {
    return gameDayPollingJob.closeEvent(eventEspnId);
  }
}
