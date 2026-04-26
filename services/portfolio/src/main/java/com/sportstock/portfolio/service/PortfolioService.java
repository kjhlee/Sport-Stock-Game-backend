package com.sportstock.portfolio.service;

import com.sportstock.common.dto.portfolio.PortfolioResponse;
import com.sportstock.portfolio.entity.Portfolio;
import com.sportstock.portfolio.mapper.PortfolioDtoMapper;
import com.sportstock.portfolio.repo.PortfolioRepo;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

  private final PortfolioRepo portfolioRepo;
  private final HoldingsService holdingsService;
  private final PortfolioHistoryService portfolioHistoryService;

  @Transactional(readOnly = true)
  public PortfolioResponse getPortfolio(Long userId, Long leagueId) {
    return PortfolioDtoMapper.toResponse(getPortfolioEntity(userId, leagueId));
  }

  @Transactional
  public PortfolioResponse upsertPortfolio(Long userId, Long leagueId) {
    Portfolio portfolio = getOrCreatePortfolio(userId, leagueId);
    return PortfolioDtoMapper.toResponse(portfolio);
  }

  @Transactional
  public void processBuy(Long userId, Long leagueId, UUID stockId, BigDecimal quantity) {
    Portfolio portfolio = getOrCreatePortfolio(userId, leagueId);
    holdingsService.addHolding(portfolio, stockId, quantity);
  }

  @Transactional
  public void processSell(Long userId, Long leagueId, UUID stockId, BigDecimal quantity) {
    Portfolio portfolio = getPortfolioEntity(userId, leagueId);
    holdingsService.reduceHolding(portfolio, stockId, quantity);
  }

  @Transactional
  public void clearHoldings(Long userId, Long leagueId) {
    Portfolio portfolio = getPortfolioEntity(userId, leagueId);
    holdingsService.clearHoldings(portfolio);
  }

  @Transactional
  public void initializeHistory(
      Long userId, Long leagueId, Integer weekNumber, String seasonType, BigDecimal startValue) {
    Portfolio portfolio = getOrCreatePortfolio(userId, leagueId);
    portfolioHistoryService.initializeHistory(portfolio, weekNumber, seasonType, startValue);
  }

  @Transactional
  public void finalizeHistory(
      Long userId, Long leagueId, Integer weekNumber, String seasonType, BigDecimal endValue) {
    Portfolio portfolio = getPortfolioEntity(userId, leagueId);
    portfolioHistoryService.finalizeHistory(portfolio, weekNumber, seasonType, endValue);
  }

  private Portfolio getPortfolioEntity(Long userId, Long leagueId) {
    return portfolioRepo
        .findByUserIdAndLeagueId(userId, leagueId)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Portfolio not found for user " + userId + " in league " + leagueId));
  }

  private Portfolio getOrCreatePortfolio(Long userId, Long leagueId) {
    return portfolioRepo
        .findByUserIdAndLeagueId(userId, leagueId)
        .orElseGet(
            () -> {
              Portfolio portfolio = new Portfolio();
              portfolio.setUserId(userId);
              portfolio.setLeagueId(leagueId);
              Portfolio saved = portfolioRepo.save(portfolio);
              log.info(
                  "Created portfolio id={} for userId={} leagueId={}",
                  saved.getId(),
                  userId,
                  leagueId);
              return saved;
            });
  }
}
