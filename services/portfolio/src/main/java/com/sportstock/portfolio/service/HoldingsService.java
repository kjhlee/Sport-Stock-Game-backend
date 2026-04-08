package com.sportstock.portfolio.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sportstock.portfolio.entity.Holdings;
import com.sportstock.portfolio.entity.Portfolio;
import com.sportstock.portfolio.repo.HoldingsRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HoldingsService {
    private static final Logger log = LoggerFactory.getLogger(HoldingsService.class);
    private final HoldingsRepo holdingsRepo;

    public void addHolding(Portfolio portfolio, UUID stockId, int quantity, BigDecimal price){
        Optional<Holdings> existing = holdingsRepo.findByPortfolio_IdAndStockId(portfolio.getId(), stockId);
        if(existing.isEmpty()){
            log.info("Creating new holding: portfolioId={} stockId={} quantity={} price={}", portfolio.getId(), stockId, quantity, price);
            Holdings newHoldings = new Holdings();
            newHoldings.setPortfolio(portfolio);
            newHoldings.setStockId(stockId);
            newHoldings.setQuantity(quantity);
            newHoldings.setAvgCostBasis(price);
            holdingsRepo.save(newHoldings);
        }
        else {
            // UPDATE THIS TO UPDATE HOLDING instead of doing the same thing
            Holdings holding = existing.get();
            log.info("Updating existing holding: portfolioId={} stockId={} addQuantity={}", portfolio.getId(), stockId, quantity);
            updateHolding(holding, portfolio, stockId, quantity, price);
        }
    }

    public void updateHolding(Holdings currHolding, Portfolio portfolio, UUID stockId, int quantity, BigDecimal price){
        currHolding.setQuantity(currHolding.getQuantity() + quantity);
        holdingsRepo.save(currHolding);
        log.info("Holding updated: holdingId={} newQuantity={}", currHolding.getId(), currHolding.getQuantity());
    }

    public void reduceHolding(Portfolio portfolio, UUID stockId, int decreaseAmmount){
        Holdings currHolding = holdingsRepo.findByPortfolio_IdAndStockId(portfolio.getId(), stockId)
                                    .orElseThrow(() -> new RuntimeException("Holding not found "));
        int currQuantity = currHolding.getQuantity();
        if (currQuantity - decreaseAmmount <= 0){
            log.info("Removing holding: portfolioId={} stockId={} (quantity would reach 0)", portfolio.getId(), stockId);
            removeHolding(currHolding);
            return;
        }
        currHolding.setQuantity(currQuantity - decreaseAmmount);
        holdingsRepo.save(currHolding);
        log.info("Reduced holding: portfolioId={} stockId={} newQuantity={}", portfolio.getId(), stockId, currHolding.getQuantity());
    }

    public void removeHolding(Holdings holding){
        log.info("Deleting holding: holdingId={}", holding.getId());
        holdingsRepo.delete(holding);
    }

    public List<Holdings> getHoldingsForPortfolio(Long portfolioId){
        List<Holdings> portfolioHoldings;
        portfolioHoldings = holdingsRepo.findByPortfolio_Id(portfolioId);
        return portfolioHoldings;
    }


}
