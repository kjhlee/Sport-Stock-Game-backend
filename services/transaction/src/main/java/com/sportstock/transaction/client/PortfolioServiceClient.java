package com.sportstock.transaction.client;

import com.sportstock.common.dto.portfolio.HoldingsResponse;
import com.sportstock.common.dto.portfolio.PortfolioHistoryRequest;
import com.sportstock.common.dto.portfolio.PortfolioResponse;
import com.sportstock.common.dto.portfolio.PortfolioUpsertRequest;
import com.sportstock.common.dto.portfolio.ProcessBuyRequest;
import com.sportstock.common.dto.portfolio.ProcessSellRequest;
import com.sportstock.transaction.exception.TransactionException;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioServiceClient {
  private final RestClient portfolioRestClient;

  public void upsertPortfolio(Long userId, Long leagueId) {
    try {
      portfolioRestClient
          .post()
          .uri("/api/internal/portfolio/upsert")
          .body(new PortfolioUpsertRequest(userId, leagueId))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException e) {
      log.error(
          "Failed to upsert portfolio for user {} league {}: {}", userId, leagueId, e.getMessage());
      throw new TransactionException("Portfolio service unavailable", e);
    }
  }

  public void processBuy(Long userId, Long leagueId, UUID stockId, BigDecimal quantity) {
    try {
      portfolioRestClient
          .post()
          .uri("/api/internal/portfolio/{leagueId}/users/{userId}/buy", leagueId, userId)
          .body(new ProcessBuyRequest(stockId, quantity))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException e) {
      log.error(
          "Failed to sync portfolio buy for user {} league {} stock {}: {}",
          userId,
          leagueId,
          stockId,
          e.getMessage());
      throw new TransactionException("Portfolio service unavailable", e);
    }
  }

  public void processSell(Long userId, Long leagueId, UUID stockId, BigDecimal quantity) {
    try {
      portfolioRestClient
          .post()
          .uri("/api/internal/portfolio/{leagueId}/users/{userId}/sell", leagueId, userId)
          .body(new ProcessSellRequest(stockId, quantity))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException e) {
      log.error(
          "Failed to sync portfolio sell for user {} league {} stock {}: {}",
          userId,
          leagueId,
          stockId,
          e.getMessage());
      throw new TransactionException("Portfolio service unavailable", e);
    }
  }

  public void clearHoldings(Long userId, Long leagueId) {
    try {
      portfolioRestClient
          .post()
          .uri("/api/internal/portfolio/{leagueId}/users/{userId}/clear", leagueId, userId)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException e) {
      log.error(
          "Failed to clear portfolio holdings for user {} league {}: {}",
          userId,
          leagueId,
          e.getMessage());
      throw new TransactionException("Portfolio service unavailable", e);
    }
  }

  public PortfolioResponse getPortfolio(Long userId, Long leagueId) {
    try {
      return portfolioRestClient
          .get()
          .uri("/api/internal/portfolio/{leagueId}/users/{userId}", leagueId, userId)
          .retrieve()
          .body(PortfolioResponse.class);
    } catch (RestClientResponseException e) {
      log.error(
          "Failed to fetch portfolio for user {} league {}: {}", userId, leagueId, e.getMessage());
      throw new TransactionException("Portfolio service unavailable", e);
    }
  }

  public HoldingsResponse getHolding(Long userId, Long leagueId, UUID stockId) {
    PortfolioResponse portfolio = getPortfolio(userId, leagueId);
    return portfolio.holdingsList().stream()
        .filter(holding -> holding.stockId().equals(stockId))
        .findFirst()
        .orElse(null);
  }

  public void initializeHistory(
      Long userId, Long leagueId, Integer weekNumber, String seasonType, BigDecimal value) {
    submitHistory(
        "/api/internal/portfolio/history/initialize",
        userId,
        leagueId,
        weekNumber,
        seasonType,
        value);
  }

  public void finalizeHistory(
      Long userId, Long leagueId, Integer weekNumber, String seasonType, BigDecimal value) {
    submitHistory(
        "/api/internal/portfolio/history/finalize",
        userId,
        leagueId,
        weekNumber,
        seasonType,
        value);
  }

  private void submitHistory(
      String uri,
      Long userId,
      Long leagueId,
      Integer weekNumber,
      String seasonType,
      BigDecimal value) {
    try {
      portfolioRestClient
          .post()
          .uri(uri)
          .body(new PortfolioHistoryRequest(userId, leagueId, weekNumber, seasonType, value))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException e) {
      log.error(
          "Failed to submit portfolio history for user {} league {} week {} seasonType {}: {}",
          userId,
          leagueId,
          weekNumber,
          seasonType,
          e.getMessage());
      throw new TransactionException("Portfolio service unavailable", e);
    }
  }
}
