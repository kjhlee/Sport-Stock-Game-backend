package com.sportstock.ingestion.mapper;

import static com.sportstock.ingestion.mapper.JsonNodeUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportstock.ingestion.entity.Event;
import com.sportstock.ingestion.entity.EventCompetitor;
import com.sportstock.ingestion.entity.EventCompetitorLinescore;
import com.sportstock.ingestion.entity.Team;
import com.sportstock.ingestion.exception.IngestionException;
import java.math.BigDecimal;
import java.time.Instant;

public final class EventMapper {

  private EventMapper() {}

  public static void applyFields(
      JsonNode node, JsonNode competition, Event event, Integer requestedSeasonType) {
    Instant now = Instant.now();

    event.setEspnId(node.path("id").asText());
    event.setEspnUid(textOrNull(node, "uid"));
    event.setName(textOrNull(node, "name"));
    event.setShortName(textOrNull(node, "shortName"));

    String rawDate = node.path("date").asText(null);
    if (rawDate == null || rawDate.isBlank()) {
      throw new IngestionException(
          "Event " + node.path("id").asText() + " is missing a parseable date");
    }
    event.setDate(parseInstantOrNull(rawDate));

    JsonNode season = node.path("season");
    if (!season.isMissingNode()) {
      event.setSeasonYear(season.path("year").asInt());
      event.setSeasonType(resolveSeasonType(season, requestedSeasonType));
      event.setSeasonSlug(textOrNull(season, "slug"));
    } else if (requestedSeasonType != null) {
      event.setSeasonType(requestedSeasonType);
    }

    JsonNode week = node.path("week");
    if (!week.isMissingNode()) {
      event.setWeekNumber(intOrNull(week, "number"));
    }

    if (competition != null && !competition.isMissingNode()) {
      event.setAttendance(intOrNull(competition, "attendance"));
      event.setNeutralSite(boolOrNull(competition, "neutralSite"));
      event.setConferenceCompetition(boolOrNull(competition, "conferenceCompetition"));
      event.setPlayByPlayAvailable(boolOrNull(competition, "playByPlayAvailable"));
    }

    JsonNode status = node.path("status");
    if (!status.isMissingNode()) {
      JsonNode statusType = status.path("type");
      if (!statusType.isMissingNode()) {
        event.setStatusState(textOrNull(statusType, "state"));
        event.setStatusCompleted(boolOrNull(statusType, "completed"));
        event.setStatusDescription(textOrNull(statusType, "description"));
      }
      event.setStatusPeriod(intOrNull(status, "period"));
      Double clock = doubleOrNull(status, "clock");
      if (clock != null) {
        event.setStatusClock(BigDecimal.valueOf(clock));
      }
    }

    if (competition != null && !competition.isMissingNode()) {
      JsonNode broadcastArray = competition.path("broadcasts");
      if (broadcastArray.isArray() && !broadcastArray.isEmpty()) {
        JsonNode firstBroadcast = broadcastArray.get(0);
        JsonNode names = firstBroadcast.path("names");
        if (names.isArray() && !names.isEmpty()) {
          event.setBroadcast(names.get(0).asText());
        }
      }
    }

    JsonNode notes = node.path("notes");
    if (notes.isArray() && !notes.isEmpty()) {
      event.setNoteHeadline(textOrNull(notes.get(0), "headline"));
    }

    if (event.getIngestedAt() == null) {
      event.setIngestedAt(now);
    }
    event.setUpdatedAt(now);
  }

  private static Integer resolveSeasonType(JsonNode seasonNode, Integer requestedSeasonType) {
    JsonNode typeNode = seasonNode.path("type");
    if (typeNode.isMissingNode() || typeNode.isNull()) {
      return requestedSeasonType;
    }

    if (typeNode.isObject()) {
      return parseSeasonTypeValue(textOrNull(typeNode, "id"), requestedSeasonType);
    }

    return parseSeasonTypeValue(typeNode.asText(null), requestedSeasonType);
  }

  private static Integer parseSeasonTypeValue(String rawValue, Integer requestedSeasonType) {
    if (rawValue == null || rawValue.isBlank()) {
      return requestedSeasonType;
    }
    try {
      return Integer.valueOf(rawValue);
    } catch (NumberFormatException e) {
      throw new IngestionException("Failed to parse season type value: " + rawValue, e);
    }
  }

  public static void applyCompetitorFields(
      JsonNode node, EventCompetitor competitor, Event event, Team team) {
    competitor.setEvent(event);
    competitor.setTeam(team);
    competitor.setHomeAway(node.path("homeAway").asText());
    competitor.setOrder(intOrNull(node, "order"));
    competitor.setWinner(boolOrNull(node, "winner"));
    competitor.setScore(textOrNull(node, "score"));

    JsonNode records = node.path("records");
    if (records.isArray()) {
      for (JsonNode record : records) {
        String type = textOrNull(record, "type");
        String summary = textOrNull(record, "summary");
        if ("total".equals(type)) {
          competitor.setRecordSummary(summary);
        } else if ("home".equals(type)) {
          competitor.setHomeRecord(summary);
        } else if ("road".equals(type)) {
          competitor.setRoadRecord(summary);
        }
      }
    }

    if (competitor.getIngestedAt() == null) {
      competitor.setIngestedAt(Instant.now());
    }
  }

  public static void applyLinescoreFields(
      JsonNode node, EventCompetitorLinescore linescore, EventCompetitor competitor) {
    linescore.setEventCompetitor(competitor);
    linescore.setPeriod(node.path("period").asInt());
    Double val = doubleOrNull(node, "value");
    if (val != null) {
      linescore.setValue(BigDecimal.valueOf(val));
    }
    linescore.setDisplayValue(textOrNull(node, "displayValue"));
  }
}
