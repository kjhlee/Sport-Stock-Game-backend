package com.sportstock.transaction.client;

import com.sportstock.common.dto.league.StipendEligibleLeagueResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeagueServiceClient {

  private final RestClient leagueRestClient;

  public List<Long> getMemberUserIdsInternal(Long leagueId) {
    try {
      return leagueRestClient
          .get()
          .uri("/api/v1/leagues/internal/{leagueId}/member-ids", leagueId)
          .retrieve()
          .body(new ParameterizedTypeReference<>() {});
    } catch (RestClientResponseException e) {
      log.error("Failed to fetch member IDs for league {}: {}", leagueId, e.getMessage());
      throw new RuntimeException("League service unavailable", e);
    }
  }

  public List<StipendEligibleLeagueResponse> getStipendEligibleLeagues(short payoutDay) {
    try {
      return leagueRestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/v1/leagues/internal/stipend-eligible")
                      .queryParam("payoutDay", payoutDay)
                      .build())
          .retrieve()
          .body(new ParameterizedTypeReference<>() {});
    } catch (RestClientResponseException e) {
      log.error("Failed to fetch stipend-eligible leagues: {}", e.getMessage());
      throw new RuntimeException("League service unavailable", e);
    }
  }
}
