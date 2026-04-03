package com.sportstock.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportstock.ingestion.client.EspnFantasyClient;
import com.sportstock.ingestion.entity.Event;
import com.sportstock.ingestion.entity.FantasySnapshot;
import com.sportstock.ingestion.mapper.FantasySnapshotMapper;
import com.sportstock.ingestion.repo.EventCompetitorRepository;
import com.sportstock.ingestion.repo.EventRepository;
import com.sportstock.ingestion.repo.FantasySnapshotRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class FantasySnapshotIngestionService {

    private final EspnFantasyClient espnFantasyClient;
    private final FantasySnapshotRepository fantasySnapshotRepository;
    private final EventRepository eventRepository;
    private final EventCompetitorRepository eventCompetitorRepository;


    @Transactional
    public IngestResult ingestProjections(int seasonYear, int seasonType, int weekNumber) {
        log.info(
                "Ingesting projections for season {} type {} week {}",
                seasonYear,
                seasonType,
                weekNumber);

        List<Event> weekEvents =
                eventRepository.findBySeasonYearAndSeasonTypeAndWeekNumberOrderByDateAsc(
                        seasonYear, seasonType, weekNumber);

        if (weekEvents.isEmpty()) {
            log.warn("No events found for week {}", weekNumber);
            return new IngestResult(0, 0, 0);
        }

        List<Event> preGameEvents =
                weekEvents.stream()
                        .filter(e -> "pre".equalsIgnoreCase(e.getStatusState()))
                        .toList();

        if (preGameEvents.isEmpty()) {
            log.warn("No pre-game events found for week {}", weekNumber);
            return new IngestResult(0, 0, 0);
        }

        Event targetEvent = preGameEvents.get(0);

        // Pre-load existing snapshots to avoid N+1 queries
        Map<String, FantasySnapshot> existingSnapshots = new HashMap<>();
        for (FantasySnapshot fs : fantasySnapshotRepository.findByEventId(targetEvent.getId())) {
            existingSnapshots.put(fs.getSubjectType() + ":" + fs.getEspnId(), fs);
        }

        JsonNode root = espnFantasyClient.fetchPlayers(seasonYear, weekNumber, seasonType);
        if (!root.isArray()) {
            log.warn("ESPN fantasy API returned non-array response");
            return new IngestResult(0, 0, 0);
        }

        int updated = 0;
        int skipped = 0;
        List<FantasySnapshot> toSave = new ArrayList<>();

        for (JsonNode playerNode : root) {
            try {
                boolean isDst = FantasySnapshotMapper.isTeamDefense(playerNode);
                String subjectType = isDst ? "TEAM_DEFENSE" : "PLAYER";
                String espnId =
                        isDst
                                ? FantasySnapshotMapper.extractProTeamId(playerNode)
                                : FantasySnapshotMapper.extractPlayerId(playerNode);
                String fullName = FantasySnapshotMapper.extractFullName(playerNode);

                BigDecimal projectedFp =
                        FantasySnapshotMapper.extractProjectedFantasyPoints(playerNode, weekNumber);
                String projectedStatsJson =
                        FantasySnapshotMapper.extractProjectedStatsJson(playerNode, weekNumber);

                if (projectedFp == null) {
                    skipped++;
                    continue;
                }

                String key = subjectType + ":" + espnId;
                FantasySnapshot snapshot =
                        existingSnapshots.computeIfAbsent(
                                key,
                                k -> {
                                    FantasySnapshot fs = new FantasySnapshot();
                                    fs.setEvent(targetEvent);
                                    fs.setSubjectType(subjectType);
                                    fs.setEspnId(espnId);
                                    return fs;
                                });

                snapshot.setFullName(fullName);
                snapshot.setProjectedFantasyPoints(projectedFp);
                snapshot.setProjectedStats(projectedStatsJson);
                toSave.add(snapshot);
                updated++;
            } catch (Exception e) {
                log.error("Failed to process player node: {}", e.getMessage());
                skipped++;
            }
        }

        fantasySnapshotRepository.saveAll(toSave);
        log.info("Projection ingestion complete: {} updated, {} skipped", updated, skipped);
        return new IngestResult(updated, skipped, 0);
    }

    @Transactional
    public IngestResult ingestActualFantasyPoints(String eventEspnId) {
        log.info("Ingesting actual fantasy points for event {}", eventEspnId);

        Event event =
                eventRepository
                        .findByEspnId(eventEspnId)
                        .orElseThrow(
                                () -> new RuntimeException("Event not found: " + eventEspnId));

        List<FantasySnapshot> snapshots =
                fantasySnapshotRepository.findIncompleteByEventEspnId(eventEspnId);

        if (snapshots.isEmpty()) {
            return new IngestResult(0, 0, 0);
        }

        List<Integer> teamIds =
                eventCompetitorRepository.findByEventId(event.getId()).stream()
                        .map(ec -> Integer.parseInt(ec.getTeam().getEspnId()))
                        .toList();

        JsonNode root =
                espnFantasyClient.fetchPlayersByTeams(
                        event.getSeasonYear(),
                        event.getWeekNumber(),
                        event.getSeasonType(),
                        teamIds);
        if (!root.isArray()) {
            return new IngestResult(0, 0, 0);
        }

        Map<String, JsonNode> playersByEspnId = new HashMap<>();
        for (JsonNode playerNode : root) {
            boolean isDst = FantasySnapshotMapper.isTeamDefense(playerNode);
            String espnId =
                    isDst
                            ? FantasySnapshotMapper.extractProTeamId(playerNode)
                            : FantasySnapshotMapper.extractPlayerId(playerNode);
            playersByEspnId.put(espnId, playerNode);
        }

        int updated = 0;
        List<FantasySnapshot> toSave = new ArrayList<>();
        for (FantasySnapshot snapshot : snapshots) {
            JsonNode playerNode = playersByEspnId.get(snapshot.getEspnId());
            if (playerNode == null) {
                continue;
            }
            BigDecimal actualFp =
                    FantasySnapshotMapper.extractActualFantasyPoints(
                            playerNode, event.getWeekNumber());
            if (actualFp != null) {
                snapshot.setActualFantasyPoints(actualFp);
                toSave.add(snapshot);
                updated++;
            }
        }

        fantasySnapshotRepository.saveAll(toSave);
        return new IngestResult(updated, 0, 0);
    }


    @Transactional
    public int markEventCompleted(String eventEspnId) {
        int count = fantasySnapshotRepository.markCompletedByEventEspnId(eventEspnId);
        log.info("Marked {} snapshots as completed for event {}", count, eventEspnId);
        return count;
    }

    public record IngestResult(int updated, int skipped, int errors) {}
}
