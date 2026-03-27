package com.sportstock.portfolio.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sportstock.portfolio.entity.Holdings;

public interface HoldingsRepo extends JpaRepository<Holdings, Long>{
    Long findByPortfolioId(Long portfolioId);
    Long findByPortfolioIdandStockId(Long portoflioId, Long stockId);
}
