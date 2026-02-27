package com.sportstock.ingestion.service;

import com.sportstock.ingestion.util.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionOrchestrationService {

    private final TeamIngestionService teamIngestionService;
    private final RosterIngestionService rosterIngestionService;
    private final AthleteIngestionService athleteIngestionService;
    private final EventIngestionService eventIngestionService;
    private final EventSummaryIngestionService eventSummaryIngestionService;
    private final RateLimiter rateLimiter;

    public void runFoundationSync(Integer seasonYear, Integer seasonType, Integer week) {
        log.info("Starting foundation sync for season {} type {} week {}", seasonYear, seasonType, week);
        eventIngestionService.ingestScoreboard(seasonYear, seasonType, week);
        teamIngestionService.ingestTeams();
        log.info("Foundation sync complete");
    }

    public void runWeeklySync(Integer seasonYear, Integer seasonType, Integer week) {
        log.info("Starting weekly sync for season {} type {} week {}", seasonYear, seasonType, week);
        eventIngestionService.ingestScoreboard(seasonYear, seasonType, week);

        var events = eventIngestionService.listEvents(seasonYear, week);
        for (var event : events) {
            eventSummaryIngestionService.ingestEventSummary(event.getEspnId());
            rateLimiter.pause();
        }
        log.info("Weekly sync complete: processed {} events", events.size());
    }

    public void runFullSync(
            Integer seasonYear,
            Integer seasonType,
            Integer week,
            Integer rosterLimit,
            Integer athletePageSize,
            Integer athletePageCount,
            List<String> teamEspnIds
    ) {
        log.info("Starting full sync for season {} type {} week {}", seasonYear, seasonType, week);

        runFoundationSync(seasonYear, seasonType, week);

        var teams = teamIngestionService.listTeams();
        for (var team : teams) {
            teamIngestionService.ingestTeamDetail(team.getEspnId());
            rateLimiter.pause();
        }

        rosterIngestionService.ingestAllRosters(seasonYear, rosterLimit, teamEspnIds);
        athleteIngestionService.ingestAthletes(athletePageSize, athletePageCount);
        runWeeklySync(seasonYear, seasonType, week);

        log.info("Full sync complete");
    }
}
