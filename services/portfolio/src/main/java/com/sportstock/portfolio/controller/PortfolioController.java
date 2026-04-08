package com.sportstock.portfolio.controller;

import com.sportstock.common.dto.portfolio.PortfolioResponse;
import com.sportstock.common.security.CurrentUserProvider;
import com.sportstock.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

  private final PortfolioService portfolioService;
  private final CurrentUserProvider currentUserProvider;

  @PostMapping("/{leagueId}")
  @ResponseStatus(HttpStatus.OK)
  public PortfolioResponse createPortfolio(@PathVariable Long leagueId) {
    Long userId = currentUserProvider.getCurrentUserId();
    return portfolioService.upsertPortfolio(userId, leagueId);
  }

  @GetMapping("/{leagueId}")
  @ResponseStatus(HttpStatus.OK)
  public PortfolioResponse getPortfolio(@PathVariable Long leagueId) {
    Long userId = currentUserProvider.getCurrentUserId();
    return portfolioService.getPortfolio(userId, leagueId);
  }
}
