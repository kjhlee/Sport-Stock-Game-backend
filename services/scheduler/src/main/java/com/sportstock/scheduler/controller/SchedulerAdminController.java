package com.sportstock.scheduler.controller;

import com.sportstock.common.dto.ingestion.EventResponse;
import com.sportstock.scheduler.client.IngestionClient;
import com.sportstock.scheduler.client.StockMarketClient;
import com.sportstock.scheduler.job.DailyCatchupJob;
import com.sportstock.scheduler.job.GameDayPollingJob;
import com.sportstock.scheduler.job.InitialStipendCheckJob;
import com.sportstock.scheduler.job.PreseasonBootstrapJob;
import com.sportstock.scheduler.job.ProjectionSyncJob;
import com.sportstock.scheduler.job.WeeklyLifecycleJob;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/scheduler")
@RequiredArgsConstructor
public class SchedulerAdminController {

  private final IngestionClient ingestionClient;
  private final StockMarketClient stockMarketClient;
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

  @PostMapping("/trigger/close-event")
  public GameDayPollingJob.CloseEventResult triggerCloseEvent(@RequestParam String eventEspnId) {
    return gameDayPollingJob.closeEvent(eventEspnId);
  }

  @PostMapping("/trigger/full-seed")
  public Map<String, Object> triggerFullSeed(
      @RequestParam Integer seasonYear,
      @RequestParam Integer seasonType,
      @RequestParam Integer weekNumber) {
    Map<String, Object> response = new LinkedHashMap<>();

    runStep(response, "preseasonBootstrap", () -> preseasonBootstrapJob.run(seasonYear), "ok");
    runStep(
        response,
        "scoreboardSync",
        () -> ingestionClient.syncScoreboard(seasonYear, seasonType, weekNumber),
        "ok");
    runStep(response, "syncAthletes", () -> stockMarketClient.syncAthletes(null), "ok");
    runStep(response, "syncTeamDefenseStocks", stockMarketClient::syncTeamDefenseStocks, "ok");
    runStep(response, "syncInjuries", () -> stockMarketClient.syncInjuries(seasonYear), "ok");
    runStep(
        response,
        "syncProjections",
        () -> ingestionClient.syncProjections(seasonYear, seasonType, weekNumber),
        "ok");
    runStep(
        response,
        "relistProjectedStocks",
        () -> stockMarketClient.relistProjectedStocks(seasonYear, seasonType, weekNumber),
        "ok");
    runStep(
        response,
        "updateProjectedPrices",
        () -> stockMarketClient.updateProjectedPrices(seasonYear, seasonType, weekNumber),
        "ok");

    List<Map<String, Object>> eventBackfillResults = new ArrayList<>();
    try {
      List<EventResponse> events = ingestionClient.getEvents(seasonYear, seasonType, weekNumber);
      for (EventResponse event : events) {
        Map<String, Object> eventResult = new LinkedHashMap<>();
        eventResult.put("eventEspnId", event.espnId());
        if (Boolean.TRUE.equals(event.statusCompleted())
            || "post".equalsIgnoreCase(event.statusState())) {
          eventResult.put("result", gameDayPollingJob.closeEvent(event.espnId()));
        } else if ("in".equalsIgnoreCase(event.statusState())) {
          ingestionClient.syncActualFantasyPoints(event.espnId());
          eventResult.put("result", "actualFantasySynced");
        } else {
          eventResult.put("result", "skipped");
        }
        eventBackfillResults.add(eventResult);
      }
      response.put("eventBackfill", eventBackfillResults);
    } catch (Exception e) {
      response.put("eventBackfill", "error: " + e.getMessage());
    }

    return response;
  }

  private void runStep(
      Map<String, Object> response, String stepName, Runnable runnable, Object successValue) {
    try {
      runnable.run();
      response.put(stepName, successValue);
    } catch (Exception e) {
      response.put(stepName, "error: " + e.getMessage());
    }
  }

  private void runStep(
      Map<String, Object> response,
      String stepName,
      java.util.function.Supplier<?> supplier,
      Object unused) {
    try {
      Object result = supplier.get();
      response.put(stepName, result != null ? result : "ok");
    } catch (Exception e) {
      response.put(stepName, "error: " + e.getMessage());
    }
  }
}
