package com.sportstock.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportstock.ingestion.entity.Season;
import com.sportstock.ingestion.entity.SeasonWeek;
import com.sportstock.ingestion.exception.IngestionException;
import com.sportstock.ingestion.mapper.SeasonMapper;
import com.sportstock.ingestion.repo.SeasonRepository;
import com.sportstock.ingestion.repo.SeasonWeekRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeasonIngestionService {

  private final SeasonRepository seasonRepository;
  private final SeasonWeekRepository seasonWeekRepository;

  @Transactional
  public void ingestSeasonAndWeeksFromScoreboard(
      Integer seasonYear, Integer seasonType, JsonNode root) {
    JsonNode leagueNode = root.path("leagues").path(0);
    if (leagueNode.isMissingNode()) {
      throw new IngestionException("Unexpected ESPN scoreboard response structure");
    }

    JsonNode seasonNode = leagueNode.path("season");
    JsonNode typeNode = seasonNode.path("type");
    String requestedSeasonTypeId = typeNode.path("id").asText(String.valueOf(seasonType));
    Map<String, Season> seasonsByType = new HashMap<>();

    int weekCount = 0;
    JsonNode calendar = leagueNode.path("calendar");
    if (calendar.isArray()) {
      for (JsonNode calendarEntry : calendar) {
        String calendarTypeValue = calendarEntry.path("value").asText();
        if (calendarTypeValue == null || calendarTypeValue.isBlank()) {
          continue;
        }

        Season season =
            seasonsByType.computeIfAbsent(
                calendarTypeValue,
                seasonTypeValue ->
                    upsertSeasonForType(
                        seasonYear, seasonNode, typeNode, requestedSeasonTypeId, seasonTypeValue));

        JsonNode entries = calendarEntry.path("entries");
        if (!entries.isArray()) {
          continue;
        }
        for (JsonNode entry : entries) {
          String weekValue = entry.path("value").asText();
          SeasonWeek sw =
              seasonWeekRepository
                  .findBySeasonIdAndSeasonTypeValueAndWeekValue(
                      season.getId(), calendarTypeValue, weekValue)
                  .orElseGet(SeasonWeek::new);
          SeasonMapper.applyWeekFields(entry, sw, season, calendarTypeValue);
          seasonWeekRepository.save(sw);
          weekCount++;
        }
      }
    }
    log.info(
        "Ingested season year {} across {} season types with {} weeks",
        seasonYear,
        seasonsByType.size(),
        weekCount);
  }

  private Season upsertSeasonForType(
      Integer seasonYear,
      JsonNode seasonNode,
      JsonNode currentTypeNode,
      String requestedSeasonTypeId,
      String targetSeasonTypeId) {
    Season season =
        seasonRepository
            .findByYearAndSeasonTypeId(seasonYear, targetSeasonTypeId)
            .orElseGet(Season::new);
    JsonNode typeNodeForSeason =
        requestedSeasonTypeId.equals(targetSeasonTypeId)
            ? currentTypeNode
            : SeasonMapper.syntheticTypeNode(targetSeasonTypeId);
    SeasonMapper.applyFields(seasonNode, typeNodeForSeason, season);
    return seasonRepository.save(season);
  }
}
