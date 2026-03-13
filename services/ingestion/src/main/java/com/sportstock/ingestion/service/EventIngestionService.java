package com.sportstock.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportstock.common.dto.ingestion.EventResponse;
import com.sportstock.ingestion.client.EspnApiClient;
import com.sportstock.ingestion.entity.Event;
import com.sportstock.ingestion.entity.EventCompetitor;
import com.sportstock.ingestion.entity.EventCompetitorLinescore;
import com.sportstock.ingestion.entity.Team;
import com.sportstock.ingestion.exception.EntityNotFoundException;
import com.sportstock.ingestion.exception.IngestionException;
import com.sportstock.ingestion.mapper.DtoMapper;
import com.sportstock.ingestion.mapper.EventMapper;
import com.sportstock.ingestion.mapper.JsonPayloadCodec;
import com.sportstock.ingestion.repo.EventCompetitorLinescoreRepository;
import com.sportstock.ingestion.repo.EventCompetitorRepository;
import com.sportstock.ingestion.repo.EventRepository;
import com.sportstock.ingestion.repo.TeamRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventIngestionService {

  private final EspnApiClient espnApiClient;
  private final EventRepository eventRepository;
  private final EventCompetitorRepository eventCompetitorRepository;
  private final EventCompetitorLinescoreRepository eventCompetitorLinescoreRepository;
  private final TeamRepository teamRepository;
  private final SeasonIngestionService seasonIngestionService;
  private final JsonPayloadCodec jsonPayloadCodec;

  @Transactional
  public void ingestScoreboard(Integer seasonYear, Integer seasonType, Integer week) {
    String json = espnApiClient.fetchScoreboard(seasonYear, seasonType, week);
    JsonNode root = jsonPayloadCodec.parseJson(json);
    JsonNode events = root.path("events");

    seasonIngestionService.ingestSeasonAndWeeksFromScoreboard(seasonYear, seasonType, root);

    if (!events.isArray()) {
      throw new IngestionException("Unexpected ESPN scoreboard response structure");
    }

    int eventCount = 0;
    for (JsonNode eventNode : events) {
      JsonNode competition = eventNode.path("competitions").path(0);

      Event event =
          eventRepository.findByEspnId(eventNode.path("id").asText()).orElseGet(Event::new);
      EventMapper.applyFields(eventNode, competition, event, seasonType);
      event = eventRepository.save(event);

      if (!competition.isMissingNode()) {
        upsertCompetitors(competition, event);
      }
      eventCount++;
    }
    log.info(
        "Ingested {} events for season {} type {} week {}",
        eventCount,
        seasonYear,
        seasonType,
        week);
  }

  public List<EventResponse> listEvents(
      Integer seasonYear, Integer seasonType, Integer weekNumber) {
    if (weekNumber != null) {
      if (seasonType == null) {
        throw new IllegalArgumentException("seasonType is required when weekNumber is provided");
      }
      return eventRepository
          .findBySeasonYearAndSeasonTypeAndWeekNumberOrderByDateAsc(
              seasonYear, seasonType, weekNumber)
          .stream()
          .map(DtoMapper::toEventResponse)
          .toList();
    }
    return eventRepository.findBySeasonYearOrderByDateAsc(seasonYear).stream()
        .map(DtoMapper::toEventResponse)
        .toList();
  }

  public EventResponse getEventByEspnId(String eventEspnId) {
    return eventRepository
        .findByEspnId(eventEspnId)
        .map(DtoMapper::toEventResponse)
        .orElseThrow(
            () -> new EntityNotFoundException("Event not found with ESPN ID: " + eventEspnId));
  }

  private void upsertCompetitors(JsonNode competition, Event event) {
    JsonNode competitors = competition.path("competitors");
    if (!competitors.isArray()) {
      return;
    }

    for (JsonNode compNode : competitors) {
      String teamEspnId = compNode.path("id").asText();
      Team team = teamRepository.findByEspnId(teamEspnId).orElse(null);
      if (team == null) {
        log.warn("Team {} not found during event competitor ingestion, skipping", teamEspnId);
        continue;
      }

      EventCompetitor competitor =
          eventCompetitorRepository
              .findByEventIdAndTeamId(event.getId(), team.getId())
              .orElseGet(EventCompetitor::new);
      EventMapper.applyCompetitorFields(compNode, competitor, event, team);
      competitor = eventCompetitorRepository.save(competitor);

      upsertLinescores(compNode, competitor);
    }
  }

  private void upsertLinescores(JsonNode compNode, EventCompetitor competitor) {
    JsonNode linescores = compNode.path("linescores");
    if (!linescores.isArray()) {
      return;
    }

    for (JsonNode lsNode : linescores) {
      int period = lsNode.path("period").asInt();
      EventCompetitorLinescore linescore =
          eventCompetitorLinescoreRepository
              .findByEventCompetitorIdAndPeriod(competitor.getId(), period)
              .orElseGet(EventCompetitorLinescore::new);
      EventMapper.applyLinescoreFields(lsNode, linescore, competitor);
      eventCompetitorLinescoreRepository.save(linescore);
    }
  }
}
