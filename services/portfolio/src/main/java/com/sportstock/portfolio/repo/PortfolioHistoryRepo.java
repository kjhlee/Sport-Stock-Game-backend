package com.sportstock.portfolio.repo;

import com.sportstock.portfolio.entity.PortfolioHistory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioHistoryRepo extends JpaRepository<PortfolioHistory, Long> {

  Optional<PortfolioHistory> findByUserIdAndLeagueIdAndWeekNumberAndSeasonType(
      Long userId, Long leagueId, Integer weekNumber, String seasonType);
}
