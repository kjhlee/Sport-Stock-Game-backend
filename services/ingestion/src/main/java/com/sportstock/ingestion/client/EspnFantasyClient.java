package com.sportstock.ingestion.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class EspnFantasyClient {
  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public EspnFantasyClient(
      @Value("${espn.api.fantasy-base-url:https://lm-api-reads.fantasy.espn.com/apis/v3/games/ffl}")
          String baseUrl,
      ObjectMapper objectMapper) {
    this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    this.objectMapper = objectMapper;
  }

  public JsonNode fetchPlayers(int seasonYear, int scoringPeriodId, int seasonType) {
    String filterHeader =
        "{\"players\":{\"limit\":2000,\"sortPercOwned\":{\"sortPriority\":4,\"sortAsc\":false}}}";

    String response =
        restClient
            .get()
            .uri(
                "/seasons/{year}/players?view=kona_player_info&scoringPeriodId={scoringPeriodId}&seasonType={seasonType}",
                seasonYear,
                scoringPeriodId,
                seasonType)
            .header("X-Fantasy-Filter", filterHeader)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(String.class);

    try {
      return extractPlayersArray(objectMapper.readTree(response));
    } catch (Exception e) {
      log.error("Failed to parse ESPN fantasy API response", e);
      throw new RuntimeException("Failed to parse ESPN fantasy response", e);
    }
  }

  public JsonNode fetchPlayersByTeams(
      int seasonYear, int scoringPeriodId, int seasonType, List<Integer> proTeamIds) {
    String filterHeader =
        String.format(
            "{\"players\":{\"limit\":200,\"filterProTeamIds\":{\"value\":%s},\"sortPercOwned\":{\"sortPriority\":4,\"sortAsc\":false}}}",
            proTeamIds);

    String response =
        restClient
            .get()
            .uri(
                "/seasons/{year}/players?view=kona_player_info&scoringPeriodId={scoringPeriodId}&seasonType={seasonType}",
                seasonYear,
                scoringPeriodId,
                seasonType)
            .header("X-Fantasy-Filter", filterHeader)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(String.class);

    try {
      return extractPlayersArray(objectMapper.readTree(response));
    } catch (Exception e) {
      log.error("Failed to parse ESPN fantasy API response", e);
      throw new RuntimeException("Failed to parse ESPN fantasy response", e);
    }
  }

  private JsonNode extractPlayersArray(JsonNode root) {
    if (root == null || root.isNull()) {
      return objectMapper.createArrayNode();
    }
    if (root.isArray()) {
      return root;
    }

    JsonNode players = root.path("players");
    if (players.isArray()) {
      return players;
    }

    // Defensive fallback for unexpected object payloads so callers can fail with context.
    ArrayNode singleEntry = objectMapper.createArrayNode();
    if (root.isObject()) {
      log.warn("Unexpected ESPN fantasy payload shape; top-level fields={}", root.fieldNames());
    }
    return singleEntry;
  }
}
