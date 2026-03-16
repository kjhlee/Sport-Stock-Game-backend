package com.sportstock.league.client;

import com.sportstock.common.dto.transaction.CreateWalletRequest;
import com.sportstock.common.dto.transaction.IssueStipendRequest;
import com.sportstock.common.dto.transaction.StipendResultResponse;
import com.sportstock.common.dto.transaction.WalletResponse;
import com.sportstock.common.exceptions.MissingAuthenticationException;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionServiceClient {

  private final RestClient transactionRestClient;

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

  public WalletResponse createWallet(Long userId, Long leagueId) {
    try {
      return transactionRestClient
          .post()
          .uri("/api/v1/wallets")
          .header("Authorization", getAuthorizationHeader())
          .body(new CreateWalletRequest(leagueId))
          .retrieve()
          .body(WalletResponse.class);
    } catch (RestClientResponseException e) {
      log.error(
          "Failed to create wallet for user {} in league {}: {}", userId, leagueId, e.getMessage());
      throw new RuntimeException("Transaction service unavailable", e);
    }
  }

  public StipendResultResponse issueInitialStipends(
      Long leagueId, BigDecimal amount, List<Long> userIds) {
    try {
      return transactionRestClient
          .post()
          .uri("/api/v1/wallets/stipends/initial")
          .header("Authorization", getAuthorizationHeader())
          .body(new IssueStipendRequest(leagueId, amount, userIds))
          .retrieve()
          .body(StipendResultResponse.class);
    } catch (RestClientResponseException e) {
      log.error("Failed to issue initial stipends for league {}: {}", leagueId, e.getMessage());
      throw new RuntimeException("Transaction service unavailable", e);
    }
  }

  public WalletResponse getWallet(Long userId, Long leagueId) {
    try {
      return transactionRestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder.path("/api/v1/wallets").queryParam("leagueId", leagueId).build())
          .header("Authorization", getAuthorizationHeader())
          .retrieve()
          .body(WalletResponse.class);
    } catch (RestClientResponseException e) {
      log.error(
          "Failed to fetch wallet for user {} in league {}: {}", userId, leagueId, e.getMessage());
      throw new RuntimeException("Transaction service unavailable", e);
    }
  }
}
