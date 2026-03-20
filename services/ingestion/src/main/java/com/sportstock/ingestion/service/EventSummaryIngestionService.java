package com.sportstock.ingestion.service;

import static com.sportstock.ingestion.mapper.JsonNodeUtils.truncate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sportstock.common.dto.ingestion.BoxscoreTeamStatResponse;
import com.sportstock.common.dto.ingestion.PlayerGameStatResponse;
import com.sportstock.ingestion.client.EspnApiClient;
import com.sportstock.ingestion.entity.Athlete;
import com.sportstock.ingestion.entity.BoxscoreTeamStat;
import com.sportstock.ingestion.entity.Event;
import com.sportstock.ingestion.entity.PlayerGameStat;
import com.sportstock.ingestion.entity.Team;
import com.sportstock.ingestion.exception.EntityNotFoundException;
import com.sportstock.ingestion.exception.IngestionException;
import com.sportstock.ingestion.mapper.AthleteMapper;
import com.sportstock.ingestion.mapper.DtoMapper;
import com.sportstock.ingestion.mapper.EventSummaryMapper;
import com.sportstock.ingestion.mapper.JsonPayloadCodec;
import com.sportstock.ingestion.repo.AthleteRepository;
import com.sportstock.ingestion.repo.BoxscoreTeamStatRepository;
import com.sportstock.ingestion.repo.EventRepository;
import com.sportstock.ingestion.repo.PlayerGameStatRepository;
import com.sportstock.ingestion.repo.TeamRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventSummaryIngestionService {

  private final EspnApiClient espnApiClient;
  private final EventRepository eventRepository;
  private final TeamRepository teamRepository;
  private final BoxscoreTeamStatRepository boxscoreTeamStatRepository;
  private final PlayerGameStatRepository playerGameStatRepository;
  private final AthleteRepository athleteRepository;
  private final JsonPayloadCodec jsonPayloadCodec;
  private final ObjectMapper objectMapper;

  @Transactional
  public void ingestEventSummary(String eventEspnId) {
    Event event =
        eventRepository
            .findByEspnIdWithLock(eventEspnId)
            .orElseThrow(
                () -> new EntityNotFoundException("Event not found with ESPN ID: " + eventEspnId));

    String json = espnApiClient.fetchEventSummary(eventEspnId);
    JsonNode root = jsonPayloadCodec.parseJson(json);

    upsertTeamStats(root, event);
    upsertPlayerStats(root, event);

    event.setIngestedAt(Instant.now());
    eventRepository.save(event);

    log.info("Ingested summary for event {}", eventEspnId);
  }

  @Transactional(readOnly = true)
  public List<BoxscoreTeamStatResponse> getTeamStats(String eventEspnId) {
    Event event =
        eventRepository
            .findByEspnId(eventEspnId)
            .orElseThrow(
                () -> new EntityNotFoundException("Event not found with ESPN ID: " + eventEspnId));
    return boxscoreTeamStatRepository.findByEventId(event.getId()).stream()
        .map(DtoMapper::toBoxscoreTeamStatResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PlayerGameStatResponse> getPlayerStats(String eventEspnId, String teamEspnId) {
    Event event =
        eventRepository
            .findByEspnId(eventEspnId)
            .orElseThrow(
                () -> new EntityNotFoundException("Event not found with ESPN ID: " + eventEspnId));

    if (teamEspnId != null && !teamEspnId.isBlank()) {
      Team team =
          teamRepository
              .findByEspnId(teamEspnId)
              .orElseThrow(
                  () -> new EntityNotFoundException("Team not found with ESPN ID: " + teamEspnId));
      return playerGameStatRepository.findByEventIdAndTeamId(event.getId(), team.getId()).stream()
          .map(e -> DtoMapper.toPlayerGameStatResponse(e, objectMapper))
          .toList();
    }
    return playerGameStatRepository.findByEventId(event.getId()).stream()
        .map(e -> DtoMapper.toPlayerGameStatResponse(e, objectMapper))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<PlayerGameStatResponse> getPlayerStatsByAthlete(
      String eventEspnId, String athleteEspnId) {
    Event event =
        eventRepository
            .findByEspnId(eventEspnId)
            .orElseThrow(
                () -> new EntityNotFoundException("Event not found with ESPN ID: " + eventEspnId));
    return playerGameStatRepository
        .findByEventIdAndAthleteEspnId(event.getId(), athleteEspnId)
        .stream()
        .map(e -> DtoMapper.toPlayerGameStatResponse(e, objectMapper))
        .toList();
  }

  private void upsertTeamStats(JsonNode root, Event event) {
    JsonNode teams = root.path("boxscore").path("teams");
    if (!teams.isArray()) {
      return;
    }

    for (JsonNode teamEntry : teams) {
      String teamEspnId = teamEntry.path("team").path("id").asText();
      if (teamEspnId == null || teamEspnId.isBlank()) {
        log.warn("Team id missing in boxscore team entry, skipping team stats");
        continue;
      }
      Team team = teamRepository.findByEspnId(teamEspnId).orElse(null);
      if (team == null) {
        log.warn("Team {} not found during boxscore stat ingestion, skipping", teamEspnId);
        continue;
      }

      String homeAway = teamEntry.path("homeAway").asText();
      JsonNode statistics = teamEntry.path("statistics");
      if (!statistics.isArray()) {
        continue;
      }

      for (JsonNode statNode : statistics) {
        String statName = statNode.path("name").asText();
        BoxscoreTeamStat stat =
            boxscoreTeamStatRepository
                .findByEventIdAndTeamIdAndStatName(event.getId(), team.getId(), statName)
                .orElseGet(BoxscoreTeamStat::new);
        EventSummaryMapper.applyBoxscoreStatFields(statNode, stat, event, team, homeAway);
        boxscoreTeamStatRepository.save(stat);
      }
    }
  }

  private void upsertPlayerStats(JsonNode root, Event event) {
    JsonNode players = root.path("boxscore").path("players");
    if (!players.isArray()) {
      return;
    }

    for (JsonNode teamPlayerEntry : players) {
      String teamEspnId = teamPlayerEntry.path("team").path("id").asText();
      if (teamEspnId == null || teamEspnId.isBlank()) {
        log.warn("Team id missing in boxscore player entry, skipping player stats");
        continue;
      }
      Team team = teamRepository.findByEspnId(teamEspnId).orElse(null);
      if (team == null) {
        log.warn("Team {} not found during player stat ingestion, skipping", teamEspnId);
        continue;
      }

      JsonNode categories = teamPlayerEntry.path("statistics");
      if (!categories.isArray()) {
        continue;
      }

      for (JsonNode category : categories) {
        String rawCategoryName = category.path("name").asText();
        String categoryName = truncate(rawCategoryName, 30);
        if (!rawCategoryName.equals(categoryName)) {
          log.warn("Truncated stat category '{}' to '{}'", rawCategoryName, categoryName);
        }
        JsonNode keys = category.path("keys");
        JsonNode athletes = category.path("athletes");
        if (!athletes.isArray()) {
          continue;
        }

        for (JsonNode athleteEntry : athletes) {
          String athleteEspnId = athleteEntry.path("athlete").path("id").asText();
          if (athleteEspnId == null || athleteEspnId.isBlank()) {
            log.warn("Athlete id missing in boxscore player entry, skipping stats");
            continue;
          }
          Athlete athlete = athleteRepository.findByEspnId(athleteEspnId).orElse(null);
          if (athlete == null) {
            athlete = createAthleteFromEventData(athleteEntry.path("athlete"), athleteEspnId);
            if (athlete == null) {
              log.warn("Could not create athlete {} from event data, skipping stats",
                      athleteEspnId);
              continue;
            }
          }

          JsonNode statsArray = athleteEntry.path("stats");
          String statsJson = buildStatsJson(keys, statsArray);

          PlayerGameStat pgs =
              playerGameStatRepository
                  .findByEventIdAndAthleteEspnIdAndStatCategory(
                      event.getId(), athleteEspnId, categoryName)
                  .orElseGet(PlayerGameStat::new);
          pgs.setEvent(event);
          pgs.setAthlete(athlete);
          pgs.setAthleteEspnId(athleteEspnId);
          pgs.setTeam(team);
          pgs.setStatCategory(categoryName);
          pgs.setStats(statsJson);

          if (pgs.getIngestedAt() == null) {
            pgs.setIngestedAt(Instant.now());
          }
          playerGameStatRepository.save(pgs);
        }
      }
    }
  }

  private String buildStatsJson(JsonNode keys, JsonNode values) {
    if (!keys.isArray() || !values.isArray()) {
      return "{}";
    }
    ObjectNode stats = objectMapper.createObjectNode();
    int len = Math.min(keys.size(), values.size());
    for (int i = 0; i < len; i++) {
      stats.put(keys.get(i).asText(), values.get(i).asText());
    }
    try {
      return objectMapper.writeValueAsString(stats);
    } catch (JsonProcessingException e) {
      throw new IngestionException("Failed to serialize player stats JSON", e);
    }
  }

  private Athlete createAthleteFromEventData (JsonNode athleteNode, String espnId) {
    try {
      Athlete athlete = new Athlete();
      athlete.setEspnId(espnId);

      AthleteMapper.applyFields(athleteNode, athlete);
      return athleteRepository.save(athlete);
    } catch (DataIntegrityViolationException e) {
      return athleteRepository.findByEspnId(espnId).orElse(null);
    }
  }
}
