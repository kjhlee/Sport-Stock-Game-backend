package com.sportstock.scheduler.job;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import com.sportstock.common.dto.league.LeagueResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.sportstock.scheduler.client.IngestionClient;
import com.sportstock.scheduler.client.LeagueClient;
import com.sportstock.scheduler.client.StockMarketClient;
import com.sportstock.scheduler.client.TransactionClient;
import com.sportstock.scheduler.repo.EventStateRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyLifecycleJob {

    private final IngestionClient ingestionClient;
    private final LeagueClient leagueClient;
    private final TransactionClient transactionClient;
    private final StockMarketClient stockMarketClient;
    private final EventStateRepository eventStateRepository;

    @Scheduled(cron = "${scheduler.weekly-lifecycle.cron}")
    public void run() {
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
            int priorWeekNum = currentWeekNum - 1;

            log.info(
                    "Running weekly lifecycle: season {}/{}, currentWeek={}, priorWeek={}",
                    seasonYear,
                    seasonType,
                    currentWeekNum,
                    priorWeekNum);

            try {
                ingestionClient.syncScoreboard(seasonYear, seasonType, currentWeekNum);
                log.info("Seeded events for season {}/{} week {}", seasonYear, seasonType, currentWeekNum);
            } catch (Exception e) {
                log.error(
                        "Failed to seed events for season {}/{} week {}: {}",
                        seasonYear,
                        seasonType,
                        currentWeekNum,
                        e.getMessage());
            }

            List<LeagueResponse> activeLeagues = leagueClient.getActiveLeagues();
            log.info("Processing {} active leagues", activeLeagues.size());

            for (LeagueResponse league : activeLeagues) {
                processLeague(league, currentWeekNum, priorWeekNum);
            }

            int unlocked = stockMarketClient.unlockAll();
            log.info("Unlocked {} stocks", unlocked);

            clearPriorWeekEventStates(priorWeekNum, seasonYear, seasonType);

            log.info("WeeklyLifecycleJob completed");
        } catch (Exception e) {
            log.warn("Skipping weekly lifecycle because dependencies are unavailable: {}", e.getMessage());
        }
    }

    private void processLeague(
            LeagueResponse league, int currentWeekNum, int priorWeekNum) {
        Long leagueId = league.id();
        try {
            if (priorWeekNum > 0) {
                transactionClient.liquidateAssets(leagueId, priorWeekNum);
                log.info("Liquidated assets for league {} week {}", leagueId, priorWeekNum);
            }
        } catch (Exception e) {
            log.warn(
                    "Failed to liquidate assets for league {} week {}: {}",
                    leagueId,
                    priorWeekNum,
                    e.getMessage());
        }

        try {
            transactionClient.issueWeeklyStipends(
                    leagueId, league.weeklyStipendAmount(), currentWeekNum);
            log.info("Issued weekly stipend for league {} week {}", leagueId, currentWeekNum);
        } catch (Exception e) {
            log.error(
                    "Failed to issue weekly stipend for league {} week {}: {}",
                    leagueId,
                    currentWeekNum,
                    e.getMessage());
        }
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
