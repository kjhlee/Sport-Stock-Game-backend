package com.sportstock.portfolio.service;

import com.sportstock.portfolio.entity.Holdings;
import com.sportstock.portfolio.entity.Portfolio;
import com.sportstock.portfolio.repo.HoldingsRepo;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HoldingsService {

  private final HoldingsRepo holdingsRepo;

  public void addHolding(Portfolio portfolio, UUID stockId, BigDecimal quantity) {
    Holdings holding =
        holdingsRepo
            .findByPortfolio_IdAndStockId(portfolio.getId(), stockId)
            .orElseGet(Holdings::new);

    if (holding.getId() == null) {
      holding.setPortfolio(portfolio);
      holding.setStockId(stockId);
      holding.setQuantity(BigDecimal.ZERO);
    }

    holding.setQuantity(holding.getQuantity().add(quantity));
    holdingsRepo.save(holding);
    log.info(
        "Added holding quantity: portfolioId={} stockId={} quantity={} newQuantity={}",
        portfolio.getId(),
        stockId,
        quantity,
        holding.getQuantity());
  }

  public void reduceHolding(Portfolio portfolio, UUID stockId, BigDecimal quantity) {
    Holdings holding =
        holdingsRepo
            .findByPortfolio_IdAndStockId(portfolio.getId(), stockId)
            .orElseThrow(
                () -> new IllegalArgumentException("Holding not found for stock " + stockId));

    BigDecimal updatedQuantity = holding.getQuantity().subtract(quantity);
    if (updatedQuantity.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Holding quantity cannot become negative");
    }
    if (updatedQuantity.compareTo(BigDecimal.ZERO) == 0) {
      holdingsRepo.delete(holding);
      log.info("Removed holding: portfolioId={} stockId={}", portfolio.getId(), stockId);
      return;
    }

    holding.setQuantity(updatedQuantity);
    holdingsRepo.save(holding);
    log.info(
        "Reduced holding quantity: portfolioId={} stockId={} quantity={} newQuantity={}",
        portfolio.getId(),
        stockId,
        quantity,
        updatedQuantity);
  }

  public void clearHoldings(Portfolio portfolio) {
    List<Holdings> holdings = holdingsRepo.findByPortfolio_Id(portfolio.getId());
    if (!holdings.isEmpty()) {
      holdingsRepo.deleteAll(holdings);
    }
    log.info("Cleared {} holdings for portfolioId={}", holdings.size(), portfolio.getId());
  }
}
