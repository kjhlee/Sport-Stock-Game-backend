package com.sportstock.portfolio.repo;

import com.sportstock.portfolio.entity.Portfolio;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioRepo extends JpaRepository<Portfolio, Long> {
  Optional<Portfolio> findByUserIdAndLeagueId(Long userId, Long leagueId);
}
