package com.sportstock.portfolio.controller;

import com.sportstock.common.dto.portfolio.PortfolioHistoryRequest;
import com.sportstock.common.dto.portfolio.PortfolioResponse;
import com.sportstock.common.dto.portfolio.PortfolioUpsertRequest;
import com.sportstock.common.dto.portfolio.ProcessBuyRequest;
import com.sportstock.common.dto.portfolio.ProcessSellRequest;
import com.sportstock.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/portfolio")
@RequiredArgsConstructor
public class InternalPortfolioController {

  private final PortfolioService portfolioService;

  @PostMapping("/upsert")
  @ResponseStatus(HttpStatus.OK)
  public PortfolioResponse upsertPortfolio(@Valid @RequestBody PortfolioUpsertRequest request) {
    return portfolioService.upsertPortfolio(request.userId(), request.leagueId());
  }

  @GetMapping("/{leagueId}/users/{userId}")
  @ResponseStatus(HttpStatus.OK)
  public PortfolioResponse getPortfolio(@PathVariable Long leagueId, @PathVariable Long userId) {
    return portfolioService.getPortfolio(userId, leagueId);
  }

  @PostMapping("/{leagueId}/users/{userId}/buy")
  @ResponseStatus(HttpStatus.OK)
  public void processBuy(
      @PathVariable Long leagueId,
      @PathVariable Long userId,
      @Valid @RequestBody ProcessBuyRequest request) {
    portfolioService.processBuy(userId, leagueId, request.stockId(), request.quantity());
  }

  @PostMapping("/{leagueId}/users/{userId}/sell")
  @ResponseStatus(HttpStatus.OK)
  public void processSell(
      @PathVariable Long leagueId,
      @PathVariable Long userId,
      @Valid @RequestBody ProcessSellRequest request) {
    portfolioService.processSell(userId, leagueId, request.stockId(), request.quantity());
  }

  @PostMapping("/{leagueId}/users/{userId}/clear")
  @ResponseStatus(HttpStatus.OK)
  public void clearHoldings(@PathVariable Long leagueId, @PathVariable Long userId) {
    portfolioService.clearHoldings(userId, leagueId);
  }

  @PostMapping("/history/initialize")
  @ResponseStatus(HttpStatus.OK)
  public void initializeHistory(@Valid @RequestBody PortfolioHistoryRequest request) {
    portfolioService.initializeHistory(
        request.userId(),
        request.leagueId(),
        request.weekNumber(),
        request.seasonType(),
        request.value());
  }

  @PostMapping("/history/finalize")
  @ResponseStatus(HttpStatus.OK)
  public void finalizeHistory(@Valid @RequestBody PortfolioHistoryRequest request) {
    portfolioService.finalizeHistory(
        request.userId(),
        request.leagueId(),
        request.weekNumber(),
        request.seasonType(),
        request.value());
  }
}
