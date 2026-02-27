package com.sportstock.ingestion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionOrchestrationService {

    private final TeamIngestionService teamIngestionService;
    private final RosterIngestionService rosterIngestionService;
    private final AthleteIngestionService athleteIngestionService;
    private final EventIngestionService eventIngestionService;
    private final EventSummaryIngestionService eventSummaryIngestionService;

    private final AtomicBoolean fullSyncRunning = new AtomicBoolean(false);
    private final AtomicInteger activeWindowJobs = new AtomicInteger(0);
    private final ConcurrentHashMap<String, AtomicBoolean> seasonWindowLocks = new ConcurrentHashMap<>();

    private boolean tryAcquireWindowJob(String jobName, String seasonWindowKey) {
        if (fullSyncRunning.get()) {
            log.warn("Skipping {} - fullSync is running", jobName);
            return false;
        }

        AtomicBoolean lock = seasonWindowLocks.computeIfAbsent(seasonWindowKey, ignored -> new AtomicBoolean(false));
        if (!lock.compareAndSet(false, true)) {
            log.warn("Skipping {} - another window job is running for {}", jobName, seasonWindowKey);
            return false;
        }

        if (fullSyncRunning.get()) {
            lock.set(false);
            seasonWindowLocks.remove(seasonWindowKey, lock);
            log.warn("Skipping {} - fullSync started while acquiring {}", jobName, seasonWindowKey);
            return false;
        }

        activeWindowJobs.incrementAndGet();
        return true;
    }

    private void releaseWindowJob(String seasonWindowKey) {
        AtomicBoolean lock = seasonWindowLocks.get(seasonWindowKey);
        if (lock != null) {
            lock.set(false);
            seasonWindowLocks.remove(seasonWindowKey, lock);
        }
        activeWindowJobs.decrementAndGet();
    }

    private boolean tryAcquireFullSync() {
        if (!fullSyncRunning.compareAndSet(false, true)) {
            log.warn("Skipping fullSync - another fullSync is already running");
            return false;
        }
        if (activeWindowJobs.get() > 0) {
            fullSyncRunning.set(false);
            log.warn("Skipping fullSync - window job(s) already running: {}", activeWindowJobs.get());
            return false;
        }
        return true;
    }

    private void releaseFullSync() {
        fullSyncRunning.set(false);
    }

    private String seasonWindowKey(Integer seasonYear, Integer seasonType, Integer week) {
        return seasonYear + "-" + seasonType + "-" + week;
    }

    @Async("ingestionExecutor")
    public void runFoundationSync(Integer seasonYear, Integer seasonType, Integer week) {
        String windowKey = seasonWindowKey(seasonYear, seasonType, week);
        if (!tryAcquireWindowJob("foundationSync", windowKey)) return;
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
        if (!tryAcquireWindowJob("weeklySync", windowKey)) return;
        try {
            log.info("Starting weekly sync for season {} type {} week {}", seasonYear, seasonType, week);
            eventIngestionService.ingestScoreboard(seasonYear, seasonType, week);

            var events = eventIngestionService.listEvents(seasonYear, week);
            for (var event : events) {
                eventSummaryIngestionService.ingestEventSummary(event.getEspnId());
            }
            log.info("Weekly sync complete: processed {} events", events.size());
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
        if (!tryAcquireFullSync()) return;
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
            releaseFullSync();
        }
    }
}
