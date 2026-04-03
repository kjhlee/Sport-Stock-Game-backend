package com.sportstock.transaction.client;

import com.sportstock.transaction.exception.TransactionException;
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
      throw new TransactionException("League service unavailable", e);
    }
  }

}
