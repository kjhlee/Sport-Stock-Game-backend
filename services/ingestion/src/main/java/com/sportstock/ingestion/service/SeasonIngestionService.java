package com.sportstock.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportstock.ingestion.client.EspnApiClient;
import com.sportstock.ingestion.entity.Season;
import com.sportstock.ingestion.entity.SeasonWeek;
import com.sportstock.ingestion.exception.IngestionException;
import com.sportstock.ingestion.mapper.JsonNodeUtils;
import com.sportstock.ingestion.mapper.SeasonMapper;
import com.sportstock.ingestion.repo.SeasonRepository;
import com.sportstock.ingestion.repo.SeasonWeekRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeasonIngestionService {

    private final EspnApiClient espnApiClient;
    private final SeasonRepository seasonRepository;
    private final SeasonWeekRepository seasonWeekRepository;

    @Transactional
    public void ingestSeasonAndWeeks(Integer seasonYear, Integer seasonType, Integer week) {
        String json = espnApiClient.fetchScoreboard(seasonYear, seasonType, week);
        JsonNode root = JsonNodeUtils.parseJson(json);

        JsonNode leagueNode = root.path("leagues").path(0);
        if (leagueNode.isMissingNode()) {
            throw new IngestionException("Unexpected ESPN scoreboard response structure");
        }

        JsonNode seasonNode = leagueNode.path("season");
        JsonNode typeNode = seasonNode.path("type");
        String seasonTypeId = typeNode.path("id").asText(String.valueOf(seasonType));

        Season season = seasonRepository.findByYearAndSeasonTypeId(seasonYear, seasonTypeId)
                .orElseGet(Season::new);
        SeasonMapper.applyFields(seasonNode, typeNode, season);
        season = seasonRepository.save(season);

        int weekCount = 0;
        JsonNode calendar = leagueNode.path("calendar");
        if (calendar.isArray()) {
            for (JsonNode calendarEntry : calendar) {
                String calendarTypeValue = calendarEntry.path("value").asText();
                JsonNode entries = calendarEntry.path("entries");
                if (!entries.isArray()) {
                    continue;
                }
                for (JsonNode entry : entries) {
                    String weekValue = entry.path("value").asText();
                    SeasonWeek sw = seasonWeekRepository
                            .findBySeasonIdAndSeasonTypeValueAndWeekValue(season.getId(), calendarTypeValue, weekValue)
                            .orElseGet(SeasonWeek::new);
                    SeasonMapper.applyWeekFields(entry, sw, season, calendarTypeValue);
                    seasonWeekRepository.save(sw);
                    weekCount++;
                }
            }
        }
        log.info("Ingested season {}/{} with {} weeks", seasonYear, seasonTypeId, weekCount);
    }
}
