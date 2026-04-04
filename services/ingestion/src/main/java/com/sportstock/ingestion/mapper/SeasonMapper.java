package com.sportstock.ingestion.mapper;

import static com.sportstock.ingestion.mapper.JsonNodeUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sportstock.ingestion.entity.Season;
import com.sportstock.ingestion.entity.SeasonWeek;
import java.time.Instant;

public final class SeasonMapper {

  private SeasonMapper() {}

  public static void applyFields(JsonNode seasonNode, JsonNode typeNode, Season season) {
    Instant now = Instant.now();

    season.setYear(seasonNode.path("year").asInt());
    season.setDisplayName(textOrNull(seasonNode, "displayName"));

    String startDate = textOrNull(seasonNode, "startDate");
    if (startDate != null) {
      season.setStartDate(parseInstantOrNull(startDate));
    }
    String endDate = textOrNull(seasonNode, "endDate");
    if (endDate != null) {
      season.setEndDate(parseInstantOrNull(endDate));
    }

    if (typeNode != null && !typeNode.isMissingNode()) {
      season.setSeasonTypeId(textOrNull(typeNode, "id"));
      season.setSeasonTypeName(textOrNull(typeNode, "name"));
      season.setSeasonTypeAbbreviation(textOrNull(typeNode, "abbreviation"));
    }

    if (season.getIngestedAt() == null) {
      season.setIngestedAt(now);
    }
    season.setUpdatedAt(now);
  }

  public static void applyWeekFields(
      JsonNode entry, SeasonWeek week, Season season, String seasonTypeValue) {
    week.setSeason(season);
    week.setSeasonTypeValue(seasonTypeValue);
    week.setWeekValue(entry.path("value").asText());
    week.setLabel(textOrNull(entry, "label"));
    week.setAlternateLabel(textOrNull(entry, "alternateLabel"));
    week.setDetail(textOrNull(entry, "detail"));

    String startDate = textOrNull(entry, "startDate");
    if (startDate != null) {
      week.setStartDate(parseInstantOrNull(startDate));
    }
    String endDate = textOrNull(entry, "endDate");
    if (endDate != null) {
      week.setEndDate(parseInstantOrNull(endDate));
    }

    if (week.getIngestedAt() == null) {
      week.setIngestedAt(Instant.now());
    }
  }

  public static JsonNode syntheticTypeNode(String seasonTypeId) {
    ObjectNode typeNode = JsonNodeFactory.instance.objectNode();
    typeNode.put("id", seasonTypeId);

    switch (seasonTypeId) {
      case "1" -> {
        typeNode.put("name", "Preseason");
        typeNode.put("abbreviation", "PRE");
      }
      case "2" -> {
        typeNode.put("name", "Regular Season");
        typeNode.put("abbreviation", "REG");
      }
      case "3" -> {
        typeNode.put("name", "Postseason");
        typeNode.put("abbreviation", "POST");
      }
      case "4" -> {
        typeNode.put("name", "Offseason");
        typeNode.put("abbreviation", "OFF");
      }
      default -> {
        typeNode.put("name", "Season Type " + seasonTypeId);
        typeNode.put("abbreviation", seasonTypeId);
      }
    }

    return typeNode;
  }
}
