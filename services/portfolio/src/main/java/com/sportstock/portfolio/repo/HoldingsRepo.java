package com.sportstock.portfolio.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sportstock.portfolio.entity.Holdings;

public interface HoldingsRepo extends JpaRepository<Holdings, Long>{
    List<Holdings> findByPortfolio_Id(Long portfolioId);
    Optional<Holdings> findByPortfolio_IdAndStockId(Long portfolioId, UUID stockId);
}
