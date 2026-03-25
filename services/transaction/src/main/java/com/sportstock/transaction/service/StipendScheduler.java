package com.sportstock.transaction.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import com.sportstock.common.dto.league.StipendEligibleLeagueResponse;
import com.sportstock.transaction.client.IngestionServiceClient;
import com.sportstock.transaction.client.LeagueServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StipendScheduler {

  private final WalletService walletService;
  private final IngestionServiceClient ingestionServiceClient;
  private final LeagueServiceClient leagueServiceClient;

  @Scheduled(cron = "0 0 0 * * *")
  public void processWeeklyStipends() {
    log.info("Weekly stipend scheduler triggered for {}", LocalDate.now().getDayOfWeek());
    CurrentWeekResponse currentWeek;
    try {
      currentWeek = ingestionServiceClient.getCurrentWeek();
    } catch (Exception e) {
      log.error("Failed to fetch current week: {}", e.getMessage());
      return;
    }
    DayOfWeek day = LocalDate.now(ZoneOffset.UTC).getDayOfWeek();
    short payoutDay = (short) (day.getValue() % 7);

    List<StipendEligibleLeagueResponse> leagues;

    try {
      leagues = leagueServiceClient.getStipendEligibleLeagues(payoutDay);
    } catch (Exception e) {
      log.error("Failed to fetch stipend-eligible leagues: {}", e.getMessage());
      return;
    }
    if (leagues.isEmpty()) {
      log.info("No leagues with payout day {} ({})", payoutDay, day);
      return;
    }

    log.info("Found {} leagues eligible for stipends today", leagues.size());

    int success = 0;
    int failed = 0;
    int skipped = 0;

    for (StipendEligibleLeagueResponse league : leagues) {
      if (league.initialStipendIssuedAt() == null) {
        log.warn("League {} has no initial stipend issued, skipping weekly", league.leagueId());
        skipped++;
        continue;
      }

      try {
        walletService.issueWeeklyStipends(league.leagueId(), league.weeklyStipendAmount(), currentWeek.week());
      } catch (Exception e) {
        log.error("Failed to issue weekly stipends for league {}: {}", league.leagueId(), e.getMessage());
        failed++;
      }
    }

    log.info("Successfully issued stipends for {} leagues, failed for {} leagues, skipped {} leagues", success, failed, skipped);

  }
}
