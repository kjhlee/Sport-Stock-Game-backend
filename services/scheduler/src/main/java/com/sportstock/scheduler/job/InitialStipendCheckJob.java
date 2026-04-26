package com.sportstock.scheduler.job;

import com.sportstock.common.dto.league.LeagueResponse;
import com.sportstock.scheduler.client.IngestionClient;
import com.sportstock.scheduler.client.LeagueClient;
import com.sportstock.scheduler.client.TransactionClient;
import com.sportstock.scheduler.season.SeasonPhase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialStipendCheckJob {

  private final IngestionClient ingestionClient;
  private final LeagueClient leagueClient;
  private final TransactionClient transactionClient;

  @Scheduled(cron = "${scheduler.initial-stipend-check.cron}")
  public void run() {
    log.info("InitialStipendCheckJob started");

    try {
      if (!ingestionClient.isSeasonActive()) {
        log.info("Season not active, skipping initial stipend check");
        return;
      }

      var currentWeek = ingestionClient.getCurrentWeek();
      SeasonPhase currentPhase = SeasonPhase.fromSeasonType(currentWeek.seasonType());
      if (!currentPhase.supportsLeagueGameplay()) {
        log.info("Skipping initial stipend check for non-gameplay phase {}", currentPhase);
        return;
      }

      List<LeagueResponse> pendingLeagues = leagueClient.getPendingInitialStipendLeagues();
      if (pendingLeagues.isEmpty()) {
        log.info("No leagues pending initial stipend");
        return;
      }

      log.info("Found {} leagues pending initial stipend", pendingLeagues.size());

      for (LeagueResponse league : pendingLeagues) {
        processLeague(league, currentWeek.seasonYear(), currentWeek.seasonType());
      }

      log.info("InitialStipendCheckJob completed");
    } catch (Exception e) {
      log.warn(
          "Skipping initial stipend check because dependencies are unavailable: {}",
          e.getMessage());
    }
  }

  private void processLeague(LeagueResponse league, Integer seasonYear, String seasonType) {
    Long leagueId = league.id();
    try {
      List<Long> memberIds = leagueClient.getMemberUserIds(leagueId);
      if (memberIds.isEmpty()) {
        log.warn("League {} has no members, skipping initial stipend", leagueId);
        return;
      }

      transactionClient.issueInitialStipends(
          leagueId, league.initialStipendAmount(), memberIds, seasonYear, seasonType);
      log.info("Issued initial stipends for league {}", leagueId);

      leagueClient.updateInitialStipendStatus(leagueId, "ISSUED");
      log.info("Updated stipend status to ISSUED for league {}", leagueId);
    } catch (Exception e) {
      log.error("Failed to process initial stipend for league {}: {}", leagueId, e.getMessage());
    }
  }
}
