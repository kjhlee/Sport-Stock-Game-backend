package com.sportstock.portfolio.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sportstock.portfolio.entity.Holdings;
import com.sportstock.portfolio.entity.Portfolio;
import com.sportstock.portfolio.repo.HoldingsRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HoldingsService {
    private final HoldingsRepo holdingsRepo;

    public void addHolding(Portfolio portfolio, UUID stockId, int quantity, BigDecimal price){
        Optional<Holdings> existing = holdingsRepo.findByPortfolio_IdAndStockId(portfolio.getId(), stockId);
        if(existing.isEmpty()){
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
            updateHolding(holding, portfolio, stockId, quantity, price);
        }
    }

    public void updateHolding(Holdings currHolding, Portfolio portfolio, UUID stockId, int quantity, BigDecimal price){
        currHolding.setQuantity(currHolding.getQuantity() + quantity);
        holdingsRepo.save(currHolding);
    }

    public void reduceHolding(Portfolio portfolio, UUID stockId, int decreaseAmmount){
        Holdings currHolding = holdingsRepo.findByPortfolio_IdAndStockId(portfolio.getId(), stockId)
                                    .orElseThrow(() -> new RuntimeException("Holding not found "));
        int currQuantity = currHolding.getQuantity();
        if (currQuantity - decreaseAmmount <= 0){
            removeHolding(currHolding);
            return;
        }
        currHolding.setQuantity(currQuantity - decreaseAmmount);
    }

    public void removeHolding(Holdings holding){
        holdingsRepo.delete(holding);
    }

    public List<Holdings> getHoldingsForPortfolio(Long portfolioId){
        List<Holdings> portfolioHoldings;
        portfolioHoldings = holdingsRepo.findByPortfolio_Id(portfolioId);
        return portfolioHoldings;
    }


}
