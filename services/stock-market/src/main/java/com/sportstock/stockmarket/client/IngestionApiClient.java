package com.sportstock.stockmarket.client;

import com.sportstock.common.dto.stock_market.IngestionAthleteDto;
import com.sportstock.common.dto.stock_market.IngestionEventDto;
import com.sportstock.common.dto.stock_market.IngestionPlayerGameStatsDto;
import com.sportstock.common.dto.stock_market.IngestionTeamDto;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
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
}
