package com.sportstock.stockmarket.client;

import com.sportstock.common.dto.ingestion.FantasySnapshotResponse;
import com.sportstock.common.dto.stock_market.IngestionAthleteDto;
import com.sportstock.common.dto.stock_market.IngestionEventDto;
import com.sportstock.common.dto.stock_market.IngestionInjuryStatusDto;
import com.sportstock.common.dto.stock_market.IngestionPlayerGameStatsDto;
import com.sportstock.common.dto.stock_market.IngestionTeamDto;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class IngestionApiClient {

  private static final ParameterizedTypeReference<List<IngestionAthleteDto>> ATHLETE_LIST =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<IngestionTeamDto>> TEAM_LIST =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<IngestionEventDto>> EVENT_LIST =
      new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<IngestionPlayerGameStatsDto>>
      PLAYER_STATS_LIST = new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<FantasySnapshotResponse>>
      FANTASY_SNAPSHOT_LIST = new ParameterizedTypeReference<>() {};
  private static final ParameterizedTypeReference<List<IngestionInjuryStatusDto>>
      INJURY_STATUS_LIST = new ParameterizedTypeReference<>() {};

  private final RestClient restClient;

  public IngestionApiClient(RestClient ingestionRestClient) {
    this.restClient = ingestionRestClient;
  }

  /** GET /athletes or GET /athletes?position={position} */
  public List<IngestionAthleteDto> getAthletes(String positionAbbreviation) {
    List<IngestionAthleteDto> body =
        restClient
            .get()
            .uri(
                uriBuilder -> {
                  uriBuilder.path("/athletes");
                  if (positionAbbreviation != null && !positionAbbreviation.isBlank()) {
                    uriBuilder.queryParam("positionAbbreviation", positionAbbreviation);
                  }
                  return uriBuilder.build();
                })
            .retrieve()
            .body(ATHLETE_LIST);

    return body != null ? body : List.of();
  }

  /** GET /athletes/{athleteEspnId} */
  public IngestionAthleteDto getAthlete(String athleteEspnId) {
    return restClient
        .get()
        .uri("/athletes/{espnId}", athleteEspnId)
        .retrieve()
        .body(IngestionAthleteDto.class);
  }

  /** GET /teams */
  public List<IngestionTeamDto> getTeams() {
    List<IngestionTeamDto> body = restClient.get().uri("/teams").retrieve().body(TEAM_LIST);

    return body != null ? body : List.of();
  }

  /** GET /events?seasonYear={year}&seasonType={type}&weekNumber={week} */
  public List<IngestionEventDto> getEvents(int seasonYear, int seasonType, int weekNumber) {
    List<IngestionEventDto> body =
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
            .body(EVENT_LIST);

    return body != null ? body : List.of();
  }

  /**
   * GET /events/{eventEspnId}/player-stats
   *
   * <p>Returns one entry per stat category per player. Caller must merge by athleteEspnId.
   */
  public List<IngestionPlayerGameStatsDto> getPlayerStats(String eventEspnId) {
    List<IngestionPlayerGameStatsDto> body =
        restClient
            .get()
            .uri("/events/{espnId}/player-stats", eventEspnId)
            .retrieve()
            .body(PLAYER_STATS_LIST);

    return body != null ? body : List.of();
  }

  public FantasySnapshotResponse getFantasySnapshot(
      String espnId, String subjectType, int seasonYear, int seasonType, int weekNumber) {
    try {
      return restClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/fantasy-snapshots/by-espn-id")
                      .queryParam("espnId", espnId)
                      .queryParam("subjectType", subjectType)
                      .queryParam("seasonYear", seasonYear)
                      .queryParam("seasonType", seasonType)
                      .queryParam("weekNumber", weekNumber)
                      .build())
          .retrieve()
          .body(FantasySnapshotResponse.class);
    } catch (HttpClientErrorException.NotFound e) {
      log.debug(
          "No fantasy snapshot found for espnId={} subjectType={} seasonYear={} seasonType={} weekNumber={}",
          espnId,
          subjectType,
          seasonYear,
          seasonType,
          weekNumber);
      return null;
    }
  }

  public List<FantasySnapshotResponse> getFantasySnapshotsByEvent(String eventEspnId) {
    List<FantasySnapshotResponse> body =
        restClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/fantasy-snapshots")
                        .queryParam("eventEspnId", eventEspnId)
                        .build())
            .retrieve()
            .body(FANTASY_SNAPSHOT_LIST);
    return body != null ? body : List.of();
  }

  /** GET /events/{espnId}/teams */
  public List<String> getEventTeamEspnIds(String eventEspnId) {
    List<String> body =
        restClient
            .get()
            .uri("/events/{espnId}/teams", eventEspnId)
            .retrieve()
            .body(new ParameterizedTypeReference<List<String>>() {});
    return body != null ? body : List.of();
  }

  public List<String> getEventRosterEspnIds(String eventEspnId, String teamEspnId) {
    List<String> body =
        restClient
            .get()
            .uri("/events/{espnId}/roster/{teamEspnId}", eventEspnId, teamEspnId)
            .retrieve()
            .body(new ParameterizedTypeReference<List<String>>() {});
    return body != null ? body : List.of();
  }

  public IngestionEventDto getEvent(String eventEspnId) {
    return restClient
        .get()
        .uri("/events/{espnId}", eventEspnId)
        .retrieve()
        .body(IngestionEventDto.class);
  }

  public List<IngestionInjuryStatusDto> getInjuredAthletes(int seasonYear) {
    List<IngestionInjuryStatusDto> body =
        restClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder.path("/rosters/injuries").queryParam("seasonYear", seasonYear).build())
            .retrieve()
            .body(INJURY_STATUS_LIST);
    return body != null ? body : List.of();
  }
}
