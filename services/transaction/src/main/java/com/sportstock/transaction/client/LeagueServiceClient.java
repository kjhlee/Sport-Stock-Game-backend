package com.sportstock.transaction.client;

import com.sportstock.common.exceptions.MissingAuthenticationException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeagueServiceClient {

  private final RestClient leagueRestClient;

  private String getAuthorizationHeader() {
    ServletRequestAttributes attrs =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs == null) {
      throw new MissingAuthenticationException("No request context available");
    }
    if (attrs.getRequest() == null) {
      throw new MissingAuthenticationException("No servlet request available");
    }
    String authorizationHeader = attrs.getRequest().getHeader("Authorization");
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      throw new MissingAuthenticationException("Missing Authorization header on incoming request");
    }
    if (!authorizationHeader.startsWith("Bearer ")) {
      throw new MissingAuthenticationException(
          "Authorization header must use Bearer token format (Authorization: Bearer <token>)");
    }
    return authorizationHeader;
  }

  public List<Long> getMemberUserIds(Long leagueId) {
    try {
      return leagueRestClient
          .get()
          .uri("/api/v1/leagues/internal/{leagueId}/member-ids", leagueId)
          .header("Authorization", getAuthorizationHeader())
          .retrieve()
          .body(new ParameterizedTypeReference<>() {});
    } catch (RestClientResponseException e) {
      log.error(
          "Failed to fetch member IDs for league {}: {}", leagueId, e.getMessage());
      throw new RuntimeException("League service unavailable", e);
    }
  }
}
