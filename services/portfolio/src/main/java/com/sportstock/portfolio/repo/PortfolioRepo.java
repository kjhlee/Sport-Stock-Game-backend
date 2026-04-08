package com.sportstock.portfolio.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sportstock.portfolio.entity.Portfolio;

public interface PortfolioRepo extends JpaRepository<Portfolio, Long>{
    Optional<Portfolio> findByUserIdAndLeagueId(Long userId, Long leagueId);
}
