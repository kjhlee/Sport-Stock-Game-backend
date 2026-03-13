package com.sportstock.league.client;

import com.sportstock.common.dto.transaction.CreateWalletRequest;
import com.sportstock.common.dto.transaction.IssueStipendRequest;
import com.sportstock.common.dto.transaction.StipendResultResponse;
import com.sportstock.common.dto.transaction.WalletResponse;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionServiceClient {

  private final RestClient transactionRestClient;

  public WalletResponse createWallet(Long userId, Long leagueId) {
    try {
      return transactionRestClient
          .post()
          .uri("/api/v1/wallets")
          .header("X-User-Id", userId.toString())
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
          .header("X-User-Id", userId.toString())
          .retrieve()
          .body(WalletResponse.class);
    } catch (RestClientResponseException e) {
      log.error(
          "Failed to fetch wallet for user {} in league {}: {}", userId, leagueId, e.getMessage());
      throw new RuntimeException("Transaction service unavailable", e);
    }
  }
}
