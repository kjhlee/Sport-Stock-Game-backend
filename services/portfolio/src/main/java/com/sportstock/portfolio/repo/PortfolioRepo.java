package com.sportstock.portfolio.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sportstock.portfolio.entity.Portfolio;

public interface PortfolioRepo extends JpaRepository<Portfolio, Long>{
    Long findByUserId(Long userId);
    Long findByUserIdAndLeagueId(Long userId, Long leagueId);
}
