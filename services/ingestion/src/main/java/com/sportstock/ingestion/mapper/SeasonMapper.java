package com.sportstock.ingestion.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportstock.ingestion.entity.Season;
import com.sportstock.ingestion.entity.SeasonWeek;

import java.time.Instant;

import static com.sportstock.ingestion.mapper.JsonNodeUtils.*;

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

    public static void applyWeekFields(JsonNode entry, SeasonWeek week, Season season, String seasonTypeValue) {
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
}
