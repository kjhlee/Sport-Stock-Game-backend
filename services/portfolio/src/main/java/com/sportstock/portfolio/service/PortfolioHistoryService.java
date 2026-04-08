package com.sportstock.portfolio.service;

import com.sportstock.portfolio.entity.Portfolio;
import com.sportstock.portfolio.entity.PortfolioHistory;
import com.sportstock.portfolio.repo.PortfolioHistoryRepo;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioHistoryService {

  private final PortfolioHistoryRepo portfolioHistoryRepo;

  public void initializeHistory(
      Portfolio portfolio, Integer weekNumber, String seasonType, BigDecimal startValue) {
    portfolioHistoryRepo
        .findByUserIdAndLeagueIdAndWeekNumberAndSeasonType(
            portfolio.getUserId(), portfolio.getLeagueId(), weekNumber, seasonType)
        .ifPresent(
            history -> {
              log.info(
                  "Portfolio history already exists for userId={} leagueId={} week={} seasonType={}",
                  portfolio.getUserId(),
                  portfolio.getLeagueId(),
                  weekNumber,
                  seasonType);
            });

    if (portfolioHistoryRepo
        .findByUserIdAndLeagueIdAndWeekNumberAndSeasonType(
            portfolio.getUserId(), portfolio.getLeagueId(), weekNumber, seasonType)
        .isPresent()) {
      return;
    }

    PortfolioHistory history = new PortfolioHistory();
    history.setPortfolio(portfolio);
    history.setUserId(portfolio.getUserId());
    history.setLeagueId(portfolio.getLeagueId());
    history.setWeekNumber(weekNumber);
    history.setSeasonType(seasonType);
    history.setStartValue(startValue);
    history.setEndValue(startValue);
    portfolioHistoryRepo.save(history);
  }

  public void finalizeHistory(
      Portfolio portfolio, Integer weekNumber, String seasonType, BigDecimal endValue) {
    PortfolioHistory history =
        portfolioHistoryRepo
            .findByUserIdAndLeagueIdAndWeekNumberAndSeasonType(
                portfolio.getUserId(), portfolio.getLeagueId(), weekNumber, seasonType)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Portfolio history not found for user "
                            + portfolio.getUserId()
                            + ", league "
                            + portfolio.getLeagueId()
                            + ", week "
                            + weekNumber
                            + ", seasonType "
                            + seasonType));
    history.setEndValue(endValue);
    portfolioHistoryRepo.save(history);
  }
}
