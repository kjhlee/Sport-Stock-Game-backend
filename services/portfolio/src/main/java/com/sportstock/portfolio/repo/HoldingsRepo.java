package com.sportstock.portfolio.repo;

import com.sportstock.portfolio.entity.Holdings;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldingsRepo extends JpaRepository<Holdings, Long> {
  List<Holdings> findByPortfolio_Id(Long portfolioId);

  Optional<Holdings> findByPortfolio_IdAndStockId(Long portfolioId, UUID stockId);
}
