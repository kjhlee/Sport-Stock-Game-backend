package com.sportstock.ingestion.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class IngestionOrchestrationService {

    private final TeamIngestionService teamIngestionService;
    private final RosterIngestionService rosterIngestionService;
    private final AthleteIngestionService athleteIngestionService;
    private final EventIngestionService eventIngestionService;
    private final EventSummaryIngestionService eventSummaryIngestionService;
    private final TransactionTemplate transactionTemplate;

    private final Object orchestrationLock = new Object();
    private boolean fullSyncRunning = false;
    private int activeWindowJobs = 0;
    private final Set<String> seasonWindowLocks = new HashSet<>();

    public IngestionOrchestrationService(
            TeamIngestionService teamIngestionService,
            RosterIngestionService rosterIngestionService,
            AthleteIngestionService athleteIngestionService,
            EventIngestionService eventIngestionService,
            EventSummaryIngestionService eventSummaryIngestionService,
            TransactionTemplate transactionTemplate
    ) {
        this.teamIngestionService = teamIngestionService;
        this.rosterIngestionService = rosterIngestionService;
        this.athleteIngestionService = athleteIngestionService;
        this.eventIngestionService = eventIngestionService;
        this.eventSummaryIngestionService = eventSummaryIngestionService;
        this.transactionTemplate = transactionTemplate;
    }

    public boolean tryStartFoundationSync(Integer seasonYear, Integer seasonType, Integer week) {
        String windowKey = seasonWindowKey(seasonYear, seasonType, week);
        return tryAcquireWindowJob("foundationSync", windowKey);
    }

    public boolean tryStartWeeklySync(Integer seasonYear, Integer seasonType, Integer week) {
        String windowKey = seasonWindowKey(seasonYear, seasonType, week);
        return tryAcquireWindowJob("weeklySync", windowKey);
    }

    public boolean tryStartFullSync() {
        return tryAcquireFullSync();
    }

    public void cancelFoundationSyncStart(Integer seasonYear, Integer seasonType, Integer week) {
        releaseWindowJob(seasonWindowKey(seasonYear, seasonType, week));
    }

    public void cancelWeeklySyncStart(Integer seasonYear, Integer seasonType, Integer week) {
        releaseWindowJob(seasonWindowKey(seasonYear, seasonType, week));
    }

    public void cancelFullSyncStart() {
        releaseFullSync();
    }

    @Async("ingestionExecutor")
    public void runFoundationSync(Integer seasonYear, Integer seasonType, Integer week) {
        String windowKey = seasonWindowKey(seasonYear, seasonType, week);
        try {
            log.info("Starting foundation sync for season {} type {} week {}", seasonYear, seasonType, week);
            eventIngestionService.ingestScoreboard(seasonYear, seasonType, week);
            teamIngestionService.ingestTeams();
            log.info("Foundation sync complete");
        } finally {
            releaseWindowJob(windowKey);
        }
    }

    @Async("ingestionExecutor")
    public void runWeeklySync(Integer seasonYear, Integer seasonType, Integer week) {
        String windowKey = seasonWindowKey(seasonYear, seasonType, week);
        try {
            log.info("Starting weekly sync for season {} type {} week {}", seasonYear, seasonType, week);
            eventIngestionService.ingestScoreboard(seasonYear, seasonType, week);

            var events = eventIngestionService.listEvents(seasonYear, week);
            int success = 0;
            int failed = 0;
            List<String> failedIds = new ArrayList<>();

            for (var event : events) {
                try {
                    transactionTemplate.executeWithoutResult(status ->
                            eventSummaryIngestionService.ingestEventSummary(event.getEspnId()));
                    success++;
                } catch (Exception e) {
                    failed++;
                    failedIds.add(event.getEspnId());
                    log.error("Failed to ingest event summary for event {}: {}", event.getEspnId(), e.getMessage());
                }
            }
            log.info("Weekly sync complete: {} events succeeded, {} failed{}",
                    success, failed, failedIds.isEmpty() ? "" : " (IDs: " + failedIds + ")");
        } finally {
            releaseWindowJob(windowKey);
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
        try {
            log.info("Starting full sync for season {} type {} week {}", seasonYear, seasonType, week);

            eventIngestionService.ingestScoreboard(seasonYear, seasonType, week);
            teamIngestionService.ingestTeams();

            var teams = (teamEspnIds != null && !teamEspnIds.isEmpty())
                    ? teamEspnIds.stream().map(teamIngestionService::getTeamByEspnId).toList()
                    : teamIngestionService.listTeams();

            int teamSuccess = 0;
            int teamFailed = 0;
            List<String> teamFailedIds = new ArrayList<>();
            for (var team : teams) {
                try {
                    transactionTemplate.executeWithoutResult(status ->
                            teamIngestionService.ingestTeamDetail(team.getEspnId(), seasonYear));
                    teamSuccess++;
                } catch (Exception e) {
                    teamFailed++;
                    teamFailedIds.add(team.getEspnId());
                    log.error("Failed to ingest team detail for {}: {}", team.getEspnId(), e.getMessage());
                }
            }
            log.info("Ingested {} team details ({} failed{})",
                    teamSuccess, teamFailed, teamFailedIds.isEmpty() ? "" : ", IDs: " + teamFailedIds);

            rosterIngestionService.ingestAllRosters(seasonYear, rosterLimit, teamEspnIds);
            athleteIngestionService.ingestAthletes(athletePageSize, athletePageCount);

            var events = eventIngestionService.listEvents(seasonYear, week);
            if (events.isEmpty()) {
                eventIngestionService.ingestScoreboard(seasonYear, seasonType, week);
                events = eventIngestionService.listEvents(seasonYear, week);
            }

            int eventSuccess = 0;
            int eventFailed = 0;
            List<String> eventFailedIds = new ArrayList<>();
            for (var event : events) {
                try {
                    transactionTemplate.executeWithoutResult(status ->
                            eventSummaryIngestionService.ingestEventSummary(event.getEspnId()));
                    eventSuccess++;
                } catch (Exception e) {
                    eventFailed++;
                    eventFailedIds.add(event.getEspnId());
                    log.error("Failed to ingest event summary for event {}: {}", event.getEspnId(), e.getMessage());
                }
            }
            log.info("Ingested {} event summaries ({} failed{})",
                    eventSuccess, eventFailed, eventFailedIds.isEmpty() ? "" : ", IDs: " + eventFailedIds);

            log.info("Full sync complete");
        } finally {
            releaseFullSync();
        }
    }

    private boolean tryAcquireWindowJob(String jobName, String seasonWindowKey) {
        synchronized (orchestrationLock) {
            if (fullSyncRunning) {
                log.warn("Rejected {} - fullSync is running", jobName);
                return false;
            }
            if (seasonWindowLocks.contains(seasonWindowKey)) {
                log.warn("Rejected {} - another window job is running for {}", jobName, seasonWindowKey);
                return false;
            }
            seasonWindowLocks.add(seasonWindowKey);
            activeWindowJobs++;
            return true;
        }
    }

    private void releaseWindowJob(String seasonWindowKey) {
        synchronized (orchestrationLock) {
            boolean removed = seasonWindowLocks.remove(seasonWindowKey);
            if (removed && activeWindowJobs > 0) {
                activeWindowJobs--;
            }
        }
    }

    private boolean tryAcquireFullSync() {
        synchronized (orchestrationLock) {
            if (fullSyncRunning) {
                log.warn("Rejected fullSync - another fullSync is already running");
                return false;
            }
            if (activeWindowJobs > 0) {
                log.warn("Rejected fullSync - {} window job(s) already running", activeWindowJobs);
                return false;
            }
            fullSyncRunning = true;
            return true;
        }
    }

    private void releaseFullSync() {
        synchronized (orchestrationLock) {
            fullSyncRunning = false;
        }
    }

    private String seasonWindowKey(Integer seasonYear, Integer seasonType, Integer week) {
        return seasonYear + "-" + seasonType + "-" + week;
    }
}
