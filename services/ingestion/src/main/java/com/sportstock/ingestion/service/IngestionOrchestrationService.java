package com.sportstock.ingestion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionOrchestrationService {

    private final TeamIngestionService teamIngestionService;
    private final RosterIngestionService rosterIngestionService;
    private final AthleteIngestionService athleteIngestionService;
    private final EventIngestionService eventIngestionService;
    private final EventSummaryIngestionService eventSummaryIngestionService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private boolean tryAcquire(String jobName) {
        if (!running.compareAndSet(false, true)) {
            log.warn("Skipping {} — a sync job is already running", jobName);
            return false;
        }
        return true;
    }

    private void release() {
        running.set(false);
    }

    @Async("ingestionExecutor")
    public void runFoundationSync(Integer seasonYear, Integer seasonType, Integer week) {
        if (!tryAcquire("foundationSync")) return;
        try {
            log.info("Starting foundation sync for season {} type {} week {}", seasonYear, seasonType, week);
            eventIngestionService.ingestScoreboard(seasonYear, seasonType, week);
            teamIngestionService.ingestTeams();
            log.info("Foundation sync complete");
        } finally {
            release();
        }
    }

    @Async("ingestionExecutor")
    public void runWeeklySync(Integer seasonYear, Integer seasonType, Integer week) {
        if (!tryAcquire("weeklySync")) return;
        try {
            log.info("Starting weekly sync for season {} type {} week {}", seasonYear, seasonType, week);
            eventIngestionService.ingestScoreboard(seasonYear, seasonType, week);

            var events = eventIngestionService.listEvents(seasonYear, week);
            for (var event : events) {
                eventSummaryIngestionService.ingestEventSummary(event.getEspnId());
            }
            log.info("Weekly sync complete: processed {} events", events.size());
        } finally {
            release();
        }
    }

    @Async("ingestionExecutor")
    public void runFullSync(
            Integer seasonYear,
            Integer seasonType,
            Integer week,
            Integer rosterLimit,
            Integer athletePageSize,
            Integer athletePageCount,
            List<String> teamEspnIds
    ) {
        if (!tryAcquire("fullSync")) return;
        try {
            log.info("Starting full sync for season {} type {} week {}", seasonYear, seasonType, week);

            eventIngestionService.ingestScoreboard(seasonYear, seasonType, week);
            teamIngestionService.ingestTeams();

            var teams = (teamEspnIds != null && !teamEspnIds.isEmpty())
                    ? teamEspnIds.stream().map(teamIngestionService::getTeamByEspnId).toList()
                    : teamIngestionService.listTeams();
            for (var team : teams) {
                teamIngestionService.ingestTeamDetail(team.getEspnId(), seasonYear);
            }

            rosterIngestionService.ingestAllRosters(seasonYear, rosterLimit, teamEspnIds);
            athleteIngestionService.ingestAthletes(athletePageSize, athletePageCount);

            var events = eventIngestionService.listEvents(seasonYear, week);
            if (events.isEmpty()) {
                eventIngestionService.ingestScoreboard(seasonYear, seasonType, week);
                events = eventIngestionService.listEvents(seasonYear, week);
            }
            for (var event : events) {
                eventSummaryIngestionService.ingestEventSummary(event.getEspnId());
            }

            log.info("Full sync complete");
        } finally {
            release();
        }
    }
}
