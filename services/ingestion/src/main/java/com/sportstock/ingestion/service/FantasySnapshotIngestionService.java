package com.sportstock.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportstock.ingestion.client.EspnFantasyClient;
import com.sportstock.ingestion.entity.Event;
import com.sportstock.ingestion.entity.EventCompetitor;
import com.sportstock.ingestion.entity.FantasySnapshot;
import com.sportstock.ingestion.entity.TeamRosterEntry;
import com.sportstock.ingestion.mapper.FantasySnapshotMapper;
import com.sportstock.ingestion.mapper.PlayerGameStatsFantasyPointCalculator;
import com.sportstock.ingestion.repo.EventCompetitorRepository;
import com.sportstock.ingestion.repo.EventRepository;
import com.sportstock.ingestion.repo.FantasySnapshotRepository;
import com.sportstock.ingestion.repo.PlayerGameStatRepository;
import com.sportstock.ingestion.repo.TeamRosterEntryRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class FantasySnapshotIngestionService {
  private static final Set<Integer> SUPPORTED_POSITION_IDS = Set.of(1, 2, 3, 4);

  private final EspnFantasyClient espnFantasyClient;
  private final FantasySnapshotRepository fantasySnapshotRepository;
  private final EventRepository eventRepository;
  private final EventCompetitorRepository eventCompetitorRepository;
  private final PlayerGameStatRepository playerGameStatRepository;
  private final PlayerGameStatsFantasyPointCalculator playerGameStatsFantasyPointCalculator;
  private final TeamRosterEntryRepository teamRosterEntryRepository;

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
      List<EventCompetitor> competitors =
          eventCompetitorRepository.findByEventEspnIdWithTeam(event.getEspnId());
      List<Integer> teamIds =
          competitors.stream().map(ec -> Integer.parseInt(ec.getTeam().getEspnId())).toList();
      Set<String> rosterAthleteIds = loadRosterAthleteIds(competitors, event.getSeasonYear());

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
      Set<String> retainedKeys = new HashSet<>();

      for (JsonNode playerNode : root) {
        try {
          ResolvedSubject subject =
              resolveSubject(playerNode, rosterAthleteIds);
          if (subject == null) {
            skipped++;
            continue;
          }

          FantasySnapshot snapshot =
              buildSnapshotFromPlayerNode(
                  playerNode,
                  existingSnapshots,
                  event,
                  weekNumber,
                  subject.subjectType(),
                  subject.espnId(),
                  subject.fullName(),
                  true);
          if (snapshot == null || !hasProjectedStats(snapshot)) {
            skipped++;
            continue;
          }
          toSave.add(snapshot);
          retainedKeys.add(subject.key());
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
      pruneSnapshots(event.getId(), retainedKeys);
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
  public IngestResult ingestProjectionsForEvent(String eventEspnId) {
    log.info("Ingesting projections for event {} (no status check)", eventEspnId);

    Event event =
        eventRepository
            .findByEspnId(eventEspnId)
            .orElseThrow(() -> new RuntimeException("Event not found: " + eventEspnId));

    List<EventCompetitor> competitors =
        eventCompetitorRepository.findByEventEspnIdWithTeam(eventEspnId);
    List<Integer> teamIds =
        competitors.stream().map(ec -> Integer.parseInt(ec.getTeam().getEspnId())).toList();
    Set<String> rosterAthleteIds = loadRosterAthleteIds(competitors, event.getSeasonYear());

    if (teamIds.isEmpty()) {
      log.warn("No competitors found for event {}", eventEspnId);
      return new IngestResult(0, 0, 0);
    }

    Map<String, FantasySnapshot> existingSnapshots = new HashMap<>();
    for (FantasySnapshot fs : fantasySnapshotRepository.findByEventId(event.getId())) {
      existingSnapshots.put(fs.getSubjectType() + ":" + fs.getEspnId(), fs);
    }

    JsonNode root =
        espnFantasyClient.fetchPlayersByTeams(
            event.getSeasonYear(), event.getWeekNumber(), event.getSeasonType(), teamIds);
    if (!root.isArray()) {
      log.warn("ESPN fantasy API returned non-array response for event {}", eventEspnId);
      return new IngestResult(0, 0, 0);
    }

    int updated = 0;
    int skipped = 0;
    List<FantasySnapshot> toSave = new ArrayList<>();
    Set<String> retainedKeys = new HashSet<>();

    for (JsonNode playerNode : root) {
      try {
        ResolvedSubject subject = resolveSubject(playerNode, rosterAthleteIds);
        if (subject == null) {
          skipped++;
          continue;
        }

        FantasySnapshot snapshot =
            buildSnapshotFromPlayerNode(
                playerNode,
                existingSnapshots,
                event,
                event.getWeekNumber(),
                subject.subjectType(),
                subject.espnId(),
                subject.fullName(),
                true);
        if (snapshot == null || !hasProjectedStats(snapshot)) {
          skipped++;
          continue;
        }
        toSave.add(snapshot);
        retainedKeys.add(subject.key());
        updated++;
      } catch (Exception e) {
        log.error(
            "Failed to process player node for event {}: {}", eventEspnId, e.getMessage());
        skipped++;
      }
    }

    fantasySnapshotRepository.saveAll(toSave);
    pruneSnapshots(event.getId(), retainedKeys);
    log.info(
        "Projection ingestion for event {}: {} updated, {} skipped",
        eventEspnId,
        updated,
        skipped);
    return new IngestResult(updated, skipped, 0);
  }

  @Transactional
  public IngestResult ingestActualFantasyPoints(String eventEspnId) {
    log.info("Ingesting actual fantasy points for event {}", eventEspnId);

    Event event =
        eventRepository
            .findByEspnId(eventEspnId)
            .orElseThrow(() -> new RuntimeException("Event not found: " + eventEspnId));

    List<EventCompetitor> competitors =
        eventCompetitorRepository.findByEventEspnIdWithTeam(eventEspnId);
    List<Integer> teamIds =
        competitors.stream()
            .map(ec -> Integer.parseInt(ec.getTeam().getEspnId()))
            .toList();
    Set<String> athleteIds =
        playerGameStatRepository.findByEventId(event.getId()).stream()
            .map(pgs -> pgs.getAthleteEspnId())
            .collect(Collectors.toSet());

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
    Set<String> retainedKeys = new HashSet<>();

    for (JsonNode playerNode : root) {
      try {
        ResolvedSubject subject = resolveSubject(playerNode, athleteIds);
        if (subject == null) {
          skipped++;
          continue;
        }

        FantasySnapshot snapshot =
            buildSnapshotFromPlayerNode(
                playerNode,
                existingSnapshots,
                event,
                event.getWeekNumber(),
                subject.subjectType(),
                subject.espnId(),
                subject.fullName(),
                false);
        if (snapshot == null) {
          skipped++;
          continue;
        }

        BigDecimal actualFp =
            playerGameStatsFantasyPointCalculator.computePlayerFantasyPoints(
                event.getId(), subject.espnId());
        if (actualFp == null) {
          actualFp = FantasySnapshotMapper.extractActualFantasyPoints(playerNode, event.getWeekNumber());
        }
        if (!hasProjectedStats(snapshot) && actualFp == null) {
          skipped++;
          continue;
        }
        snapshot.setActualFantasyPoints(actualFp);
        toSave.add(snapshot);
        retainedKeys.add(subject.key());
        updated++;
      } catch (Exception e) {
        log.error(
            "Failed to process actual fantasy node for event {}: {}",
            eventEspnId,
            e.getMessage());
        skipped++;
      }
    }

    fantasySnapshotRepository.saveAll(toSave);
    pruneSnapshots(event.getId(), retainedKeys);

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

  public Map<String, Object> debugFantasyForEvent(
      String eventEspnId, String subjectType, String espnId) {
    Event event =
        eventRepository
            .findByEspnId(eventEspnId)
            .orElseThrow(() -> new RuntimeException("Event not found: " + eventEspnId));

    List<EventCompetitor> competitors =
        eventCompetitorRepository.findByEventEspnIdWithTeam(eventEspnId);
    List<Integer> teamIds =
        competitors.stream()
            .map(ec -> Integer.parseInt(ec.getTeam().getEspnId()))
            .toList();
    Set<String> athleteIds =
        playerGameStatRepository.findByEventId(event.getId()).stream()
            .map(pgs -> pgs.getAthleteEspnId())
            .collect(Collectors.toSet());
    JsonNode root =
        espnFantasyClient.fetchPlayersByTeams(
            event.getSeasonYear(), event.getWeekNumber(), event.getSeasonType(), teamIds);
    if (!root.isArray()) {
      throw new RuntimeException("ESPN fantasy API returned non-array response for " + eventEspnId);
    }

    String normalizedSubjectType = normalizeSubjectType(subjectType);
    for (JsonNode playerNode : root) {
      ResolvedSubject subject = resolveSubject(playerNode, athleteIds);
      if (subject == null
          || !normalizedSubjectType.equals(subject.subjectType())
          || !espnId.equals(subject.espnId())) {
        continue;
      }

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("eventEspnId", eventEspnId);
      response.put("subjectType", subject.subjectType());
      response.put("espnId", subject.espnId());
      response.put("fullName", FantasySnapshotMapper.extractFullName(playerNode));
      response.put(
          "storedSnapshot",
          fantasySnapshotRepository
              .findByEventIdAndSubjectTypeAndEspnId(
                  event.getId(), subject.subjectType(), subject.espnId())
              .map(
                  snapshot -> {
                    Map<String, Object> storedSnapshot = new LinkedHashMap<>();
                    storedSnapshot.put("projectedFantasyPoints", snapshot.getProjectedFantasyPoints());
                    storedSnapshot.put("actualFantasyPoints", snapshot.getActualFantasyPoints());
                    storedSnapshot.put("completed", snapshot.isCompleted());
                    return storedSnapshot;
                  })
              .orElse(null));
      response.put(
          "projected",
          FantasySnapshotMapper.explainFantasyPoints(playerNode, 1, event.getWeekNumber()));
      response.put(
          "actual", FantasySnapshotMapper.explainFantasyPoints(playerNode, 0, event.getWeekNumber()));
      return response;
    }

    throw new RuntimeException(
        "No fantasy payload node found for "
            + normalizedSubjectType
            + " "
            + espnId
            + " in event "
            + eventEspnId);
  }

  public Map<String, Object> debugFantasyForTeam(
      String eventEspnId, String teamEspnId, String subjectType) {
    Event event =
        eventRepository
            .findByEspnId(eventEspnId)
            .orElseThrow(() -> new RuntimeException("Event not found: " + eventEspnId));

    List<EventCompetitor> competitors =
        eventCompetitorRepository.findByEventEspnIdWithTeam(eventEspnId);
    List<Integer> teamIds =
        competitors.stream()
            .map(ec -> Integer.parseInt(ec.getTeam().getEspnId()))
            .toList();
    Set<String> teamAthleteIds =
        playerGameStatRepository.findByEventId(event.getId()).stream()
            .filter(pgs -> teamEspnId.equals(pgs.getTeam().getEspnId()))
            .map(pgs -> pgs.getAthleteEspnId())
            .collect(Collectors.toSet());

    JsonNode root =
        espnFantasyClient.fetchPlayersByTeams(
            event.getSeasonYear(), event.getWeekNumber(), event.getSeasonType(), teamIds);
    if (!root.isArray()) {
      throw new RuntimeException("ESPN fantasy API returned non-array response for " + eventEspnId);
    }

    String normalizedSubjectType = normalizeSubjectTypeFilter(subjectType);
    List<Map<String, Object>> results = new ArrayList<>();

    for (JsonNode playerNode : root) {
      ResolvedSubject subject = resolveSubject(playerNode, teamAthleteIds);
      if (subject == null
          || (!"ALL".equals(normalizedSubjectType)
              && !normalizedSubjectType.equals(subject.subjectType()))) {
        continue;
      }

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("subjectType", subject.subjectType());
      response.put("espnId", subject.espnId());
      response.put("teamEspnId", teamEspnId);
      response.put("fullName", FantasySnapshotMapper.extractFullName(playerNode));
      response.put(
          "storedSnapshot",
          fantasySnapshotRepository
              .findByEventIdAndSubjectTypeAndEspnId(
                  event.getId(), subject.subjectType(), subject.espnId())
              .map(
                  snapshot -> {
                    Map<String, Object> storedSnapshot = new LinkedHashMap<>();
                    storedSnapshot.put("projectedFantasyPoints", snapshot.getProjectedFantasyPoints());
                    storedSnapshot.put("actualFantasyPoints", snapshot.getActualFantasyPoints());
                    storedSnapshot.put("completed", snapshot.isCompleted());
                    return storedSnapshot;
                  })
              .orElse(null));
      response.put(
          "projected",
          FantasySnapshotMapper.explainFantasyPoints(playerNode, 1, event.getWeekNumber()));
      response.put(
          "actual", FantasySnapshotMapper.explainFantasyPoints(playerNode, 0, event.getWeekNumber()));
      results.add(response);
    }

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("eventEspnId", eventEspnId);
    payload.put("teamEspnId", teamEspnId);
    payload.put("subjectType", normalizedSubjectType);
    payload.put("count", results.size());
    payload.put("entries", results);
    return payload;
  }

  private FantasySnapshot buildSnapshotFromPlayerNode(
      JsonNode playerNode,
      Map<String, FantasySnapshot> existingSnapshots,
      Event event,
      int scoringPeriodId,
      String subjectType,
      String espnId,
      String fullName,
      boolean refreshProjectedFields) {
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

    snapshot.setFullName(
        fullName != null && !fullName.isBlank()
            ? fullName
            : FantasySnapshotMapper.extractFullName(playerNode));
    if (refreshProjectedFields) {
      snapshot.setProjectedFantasyPoints(
          FantasySnapshotMapper.extractProjectedFantasyPoints(playerNode, scoringPeriodId));
      snapshot.setProjectedStats(
          FantasySnapshotMapper.extractProjectedStatsJson(playerNode, scoringPeriodId));
    }
    return snapshot;
  }

  private boolean hasProjectedStats(FantasySnapshot snapshot) {
    return snapshot.getProjectedStats() != null && !snapshot.getProjectedStats().isBlank();
  }

  private String normalizeSubjectType(String subjectType) {
    if ("TEAM_DEFENSE".equalsIgnoreCase(subjectType) || "DST".equalsIgnoreCase(subjectType)) {
      return "TEAM_DEFENSE";
    }
    return "PLAYER";
  }

  private String normalizeSubjectTypeFilter(String subjectType) {
    if (subjectType == null || subjectType.isBlank() || "ALL".equalsIgnoreCase(subjectType)) {
      return "ALL";
    }
    return normalizeSubjectType(subjectType);
  }

  private Set<String> loadRosterAthleteIds(List<EventCompetitor> competitors, int seasonYear) {
    return competitors.stream()
        .map(
            ec ->
                teamRosterEntryRepository.findByTeamEspnIdAndSeasonYear(
                    ec.getTeam().getEspnId(), seasonYear))
        .flatMap(Collection::stream)
        .map(TeamRosterEntry::getAthlete)
        .filter(java.util.Objects::nonNull)
        .map(athlete -> athlete.getEspnId())
        .collect(Collectors.toSet());
  }

  private Set<String> loadCanonicalDefenseIds(List<EventCompetitor> competitors) {
    return competitors.stream().map(ec -> ec.getTeam().getEspnId()).collect(Collectors.toSet());
  }

  private ResolvedSubject resolveSubject(JsonNode playerNode, Set<String> playerAllowlist) {
    if (!isSupportedFantasyPlayer(playerNode)) {
      return null;
    }
    String playerId = FantasySnapshotMapper.extractPlayerId(playerNode);
    if (playerId == null || !playerAllowlist.contains(playerId)) {
      return null;
    }
    return new ResolvedSubject("PLAYER", playerId, null);
  }

  private boolean isSupportedFantasyPlayer(JsonNode playerNode) {
    if (FantasySnapshotMapper.isTeamDefense(playerNode)) {
      return false;
    }
    int defaultPositionId = playerNode.path("defaultPositionId").asInt(-1);
    return SUPPORTED_POSITION_IDS.contains(defaultPositionId);
  }

  private String resolveCanonicalDefenseId(JsonNode playerNode, List<EventCompetitor> competitors) {
    String rawProTeamId = FantasySnapshotMapper.extractProTeamId(playerNode);
    if (rawProTeamId != null
        && competitors.stream().anyMatch(ec -> rawProTeamId.equals(ec.getTeam().getEspnId()))) {
      return rawProTeamId;
    }

    String normalizedNodeText = normalizeTeamText(extractDefenseNodeText(playerNode));
    if (normalizedNodeText.isBlank()) {
      return null;
    }

    List<Map.Entry<EventCompetitor, Integer>> ranked =
        competitors.stream()
            .map(ec -> Map.entry(ec, teamMatchScore(normalizedNodeText, ec)))
            .filter(entry -> entry.getValue() > 0)
            .sorted(Map.Entry.<EventCompetitor, Integer>comparingByValue(Comparator.reverseOrder()))
            .toList();

    if (ranked.isEmpty()) {
      return null;
    }
    if (ranked.size() == 1 || ranked.get(0).getValue() > ranked.get(1).getValue()) {
      return ranked.get(0).getKey().getTeam().getEspnId();
    }
    return null;
  }

  private String extractDefenseNodeText(JsonNode playerNode) {
    List<String> candidates =
        List.of(
            playerNode.path("fullName").asText(""),
            playerNode.path("lastName").asText(""),
            playerNode.path("displayName").asText(""),
            playerNode.path("proTeam").path("name").asText(""),
            playerNode.path("proTeam").path("displayName").asText(""),
            playerNode.path("proTeam").path("abbrev").asText(""),
            playerNode.path("proTeamAbbreviation").asText(""));
    return candidates.stream().filter(s -> !s.isBlank()).findFirst().orElse("");
  }

  private int teamMatchScore(String normalizedNodeText, EventCompetitor competitor) {
    int score = 0;
    for (String candidate : teamMatchCandidates(competitor)) {
      if (candidate.isBlank()) {
        continue;
      }
      if (normalizedNodeText.equals(candidate)) {
        score = Math.max(score, 100);
      } else if (normalizedNodeText.contains(candidate) || candidate.contains(normalizedNodeText)) {
        score = Math.max(score, candidate.length());
      }
    }
    return score;
  }

  private List<String> teamMatchCandidates(EventCompetitor competitor) {
    String location = Optional.ofNullable(competitor.getTeam().getLocation()).orElse("");
    String nickname = Optional.ofNullable(competitor.getTeam().getNickname()).orElse("");
    return List.of(
        normalizeTeamText(competitor.getTeam().getDisplayName()),
        normalizeTeamText(competitor.getTeam().getShortDisplayName()),
        normalizeTeamText(competitor.getTeam().getName()),
        normalizeTeamText(competitor.getTeam().getNickname()),
        normalizeTeamText(competitor.getTeam().getLocation()),
        normalizeTeamText(competitor.getTeam().getAbbreviation()),
        normalizeTeamText(location + nickname));
  }

  private String normalizeTeamText(String value) {
    if (value == null) {
      return "";
    }
    return value.toLowerCase().replaceAll("[^a-z0-9]", "");
  }

  private String resolveDefenseDisplayName(
      String defenseId, List<EventCompetitor> competitors) {
    return competitors.stream()
        .map(EventCompetitor::getTeam)
        .filter(team -> defenseId.equals(team.getEspnId()))
        .map(team -> team.getDisplayName() + " D/ST")
        .findFirst()
        .orElse(null);
  }

  private void pruneSnapshots(Long eventId, Set<String> retainedKeys) {
    List<FantasySnapshot> toDelete =
        fantasySnapshotRepository.findByEventId(eventId).stream()
            .filter(fs -> !retainedKeys.contains(fs.getSubjectType() + ":" + fs.getEspnId()))
            .toList();
    if (!toDelete.isEmpty()) {
      fantasySnapshotRepository.deleteAll(toDelete);
    }
  }

  private record ResolvedSubject(String subjectType, String espnId, String fullName) {
    private String key() {
      return subjectType + ":" + espnId;
    }
  }

  public record IngestResult(int updated, int skipped, int errors) {}
}
