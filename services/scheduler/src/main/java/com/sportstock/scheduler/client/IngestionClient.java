package com.sportstock.scheduler.client;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import com.sportstock.common.dto.ingestion.EventResponse;
import com.sportstock.common.dto.stock_market.IngestionEventDto;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
public class IngestionClient {

    private final RestClient restClient;

    public CurrentWeekResponse getCurrentWeek() {
        return restClient
                .get()
                .uri("/seasons/current-week")
                .retrieve()
                .body(CurrentWeekResponse.class);
    }

    public List<EventResponse> getEvents(int seasonYear, int seasonType, int weekNumber) {
        List<EventResponse> body =
                restClient
                        .get()
                        .uri(
                                uriBuilder ->
                                        uriBuilder
                                                .path("/events")
                                                .queryParam("seasonYear", seasonYear)
                                                .queryParam("seasonType", seasonType)
                                                .queryParam("weekNumber", weekNumber)
                                                .build())
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<EventResponse>>() {});
        return body != null ? body : List.of();
    }

    public IngestionEventDto getEvent(String eventEspnId) {
        return restClient
                .get()
                .uri("/events/{eventEspnId}", eventEspnId)
                .retrieve()
                .body(IngestionEventDto.class);
    }

    public void syncProjections(int seasonYear, int seasonType, int weekNumber) {
        restClient
                .post()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/sync/projections")
                                        .queryParam("seasonYear", seasonYear)
                                        .queryParam("seasonType", seasonType)
                                        .queryParam("weekNumber", weekNumber)
                                        .build())
                .retrieve()
                .toBodilessEntity();
        log.info("Synced projections for {}/{} week {}", seasonYear, seasonType, weekNumber);
    }

    public void syncTeams() {
        restClient.post().uri("/sync/teams").retrieve().toBodilessEntity();
        log.info("Synced teams");
    }

    public void syncAllTeamDetails(int seasonYear) {
        restClient
                .post()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/sync/teams/details")
                                        .queryParam("seasonYear", seasonYear)
                                        .build())
                .retrieve()
                .toBodilessEntity();
        log.info("Synced all team details for season {}", seasonYear);
    }

    public void syncRosters(int seasonYear, int rosterLimit) {
        restClient
                .post()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/sync/rosters")
                                        .queryParam("seasonYear", seasonYear)
                                        .queryParam("rosterLimit", rosterLimit)
                                        .build())
                .retrieve()
                .toBodilessEntity();
        log.info("Synced rosters for season {} with rosterLimit {}", seasonYear, rosterLimit);
    }

    public void syncStaleRosters(int seasonYear, int staleHours) {
        restClient
                .post()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/sync/rosters/stale")
                                        .queryParam("seasonYear", seasonYear)
                                        .queryParam("staleHours", staleHours)
                                        .build())
                .retrieve()
                .toBodilessEntity();
        log.info("Synced stale rosters for season {} with staleHours {}", seasonYear, staleHours);
    }

    public Map<String, Object> syncEventSummary(String eventEspnId) {
        Map<String, Object> body =
                restClient
                .post()
                .uri("/sync/events/{eventEspnId}/summary", eventEspnId)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        log.info("Synced event summary for event {}", eventEspnId);
        return body != null ? body : Map.of();
    }

    public Map<String, Object> syncActualFantasyPoints(String eventEspnId) {
        Map<String, Object> body =
                restClient
                .post()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/sync/actual-fantasy-points")
                                        .queryParam("eventEspnId", eventEspnId)
                                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        log.info("Synced actual fantasy points for event {}", eventEspnId);
        return body != null ? body : Map.of();
    }

    public Map<String, Object> markEventCompleted(String eventEspnId) {
        Map<String, Object> body =
                restClient
                .post()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/sync/mark-event-completed")
                                        .queryParam("eventEspnId", eventEspnId)
                                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        log.info("Marked event {} completed", eventEspnId);
        return body != null ? body : Map.of();
    }

    public void syncScoreboard(int seasonYear, int seasonType, int week) {
        restClient
                .post()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/sync/scoreboard")
                                        .queryParam("seasonYear", seasonYear)
                                        .queryParam("seasonType", seasonType)
                                        .queryParam("week", week)
                                        .build())
                .retrieve()
                .toBodilessEntity();
        log.info("Synced scoreboard for {}/{} week {}", seasonYear, seasonType, week);
    }

    @SuppressWarnings("unchecked")
    public boolean isSeasonActive() {
        Map<String, Object> body =
                restClient
                        .get()
                        .uri("/seasons/season-active")
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        return body != null && Boolean.TRUE.equals(body.get("active"));
    }
}
