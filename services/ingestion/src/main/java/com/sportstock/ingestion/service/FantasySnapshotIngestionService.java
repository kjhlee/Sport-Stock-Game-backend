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
        "Ingesting projections for season {} type {} week {}", seasonYear, seasonType, weekNumber);

    List<Event> weekEvents =
        eventRepository.findBySeasonYearAndSeasonTypeAndWeekNumberOrderByDateAsc(
            seasonYear, seasonType, weekNumber);

    if (weekEvents.isEmpty()) {
      log.warn("No events found for week {}", weekNumber);
      return new IngestResult(0, 0, 0);
    }

    List<Event> preGameEvents =
        weekEvents.stream().filter(e -> "pre".equalsIgnoreCase(e.getStatusState())).toList();

    if (preGameEvents.isEmpty()) {
      log.warn("No pre-game events found for week {}", weekNumber);
      return new IngestResult(0, 0, 0);
    }

    int totalUpdated = 0;
    int totalSkipped = 0;

    for (Event event : preGameEvents) {
      List<Integer> teamIds =
          eventCompetitorRepository.findByEventId(event.getId()).stream()
              .map(ec -> Integer.parseInt(ec.getTeam().getEspnId()))
              .toList();

      if (teamIds.isEmpty()) {
        log.warn("No competitors found for event {}", event.getEspnId());
        continue;
      }

      Map<String, FantasySnapshot> existingSnapshots = new HashMap<>();
      for (FantasySnapshot fs : fantasySnapshotRepository.findByEventId(event.getId())) {
        existingSnapshots.put(fs.getSubjectType() + ":" + fs.getEspnId(), fs);
      }

      JsonNode root =
          espnFantasyClient.fetchPlayersByTeams(seasonYear, weekNumber, seasonType, teamIds);
      if (!root.isArray()) {
        log.warn("ESPN fantasy API returned non-array response for event {}", event.getEspnId());
        continue;
      }

      int updated = 0;
      int skipped = 0;
      List<FantasySnapshot> toSave = new ArrayList<>();

      for (JsonNode playerNode : root) {
        try {
          FantasySnapshot snapshot =
              buildSnapshotFromPlayerNode(playerNode, existingSnapshots, event, weekNumber);
          if (snapshot == null || snapshot.getProjectedFantasyPoints() == null) {
            skipped++;
            continue;
          }
          toSave.add(snapshot);
          updated++;
        } catch (Exception e) {
          log.error(
              "Failed to process player node for event {}: {}",
              event.getEspnId(),
              e.getMessage());
          skipped++;
        }
      }

      fantasySnapshotRepository.saveAll(toSave);
      log.info(
          "Projection ingestion for event {}: {} updated, {} skipped",
          event.getEspnId(),
          updated,
          skipped);
      totalUpdated += updated;
      totalSkipped += skipped;
    }

    log.info(
        "Projection ingestion complete for week {}: {} total updated, {} total skipped",
        weekNumber,
        totalUpdated,
        totalSkipped);
    return new IngestResult(totalUpdated, totalSkipped, 0);
  }

  @Transactional
  public IngestResult ingestActualFantasyPoints(String eventEspnId) {
    log.info("Ingesting actual fantasy points for event {}", eventEspnId);

    Event event =
        eventRepository
            .findByEspnId(eventEspnId)
            .orElseThrow(() -> new RuntimeException("Event not found: " + eventEspnId));

    List<Integer> teamIds =
        eventCompetitorRepository.findByEventId(event.getId()).stream()
            .map(ec -> Integer.parseInt(ec.getTeam().getEspnId()))
            .toList();

    JsonNode root =
        espnFantasyClient.fetchPlayersByTeams(
            event.getSeasonYear(), event.getWeekNumber(), event.getSeasonType(), teamIds);
    if (!root.isArray()) {
      return new IngestResult(0, 0, 0);
    }

    Map<String, FantasySnapshot> existingSnapshots = new HashMap<>();
    for (FantasySnapshot snapshot : fantasySnapshotRepository.findByEventId(event.getId())) {
      existingSnapshots.put(snapshot.getSubjectType() + ":" + snapshot.getEspnId(), snapshot);
    }

    int updated = 0;
    int skipped = 0;
    List<FantasySnapshot> toSave = new ArrayList<>();

    for (JsonNode playerNode : root) {
      try {
        FantasySnapshot snapshot =
            buildSnapshotFromPlayerNode(playerNode, existingSnapshots, event, event.getWeekNumber());
        if (snapshot == null) {
          skipped++;
          continue;
        }

        BigDecimal actualFp =
            FantasySnapshotMapper.extractActualFantasyPoints(playerNode, event.getWeekNumber());
        if (actualFp != null) {
          snapshot.setActualFantasyPoints(actualFp);
          updated++;
        } else {
          skipped++;
        }

        toSave.add(snapshot);
      } catch (Exception e) {
        log.error(
            "Failed to process actual fantasy node for event {}: {}",
            eventEspnId,
            e.getMessage());
        skipped++;
      }
    }

    fantasySnapshotRepository.saveAll(toSave);

    log.info(
        "Final actual fantasy ingestion for event {} refreshed {} subjects ({} without actual points)",
        eventEspnId,
        updated,
        skipped);

    return new IngestResult(updated, skipped, 0);
  }

  @Transactional
  public int markEventCompleted(String eventEspnId) {
    int count = fantasySnapshotRepository.markCompletedByEventEspnId(eventEspnId);
    log.info("Marked {} snapshots as completed for event {}", count, eventEspnId);
    return count;
  }

  private FantasySnapshot buildSnapshotFromPlayerNode(
      JsonNode playerNode,
      Map<String, FantasySnapshot> existingSnapshots,
      Event event,
      int scoringPeriodId) {
    boolean isDst = FantasySnapshotMapper.isTeamDefense(playerNode);
    String subjectType = isDst ? "TEAM_DEFENSE" : "PLAYER";
    String espnId =
        isDst
            ? FantasySnapshotMapper.extractProTeamId(playerNode)
            : FantasySnapshotMapper.extractPlayerId(playerNode);

    if (espnId == null || espnId.isBlank()) {
      return null;
    }

    String key = subjectType + ":" + espnId;
    FantasySnapshot snapshot =
        existingSnapshots.computeIfAbsent(
            key,
            k -> {
              FantasySnapshot fs = new FantasySnapshot();
              fs.setEvent(event);
              fs.setSubjectType(subjectType);
              fs.setEspnId(espnId);
              return fs;
            });

    snapshot.setFullName(FantasySnapshotMapper.extractFullName(playerNode));
    snapshot.setProjectedFantasyPoints(
        FantasySnapshotMapper.extractProjectedFantasyPoints(playerNode, scoringPeriodId));
    snapshot.setProjectedStats(
        FantasySnapshotMapper.extractProjectedStatsJson(playerNode, scoringPeriodId));
    return snapshot;
  }

  public record IngestResult(int updated, int skipped, int errors) {}
}
