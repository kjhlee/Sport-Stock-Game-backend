package com.sportstock.portfolio.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.sportstock.portfolio.entity.Portfolio;
import com.sportstock.portfolio.repo.PortfolioRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepo portfolioRepo;
    private final HoldingsService holdingsService;

    public Portfolio getPortfolio(Long userId, Long leagueId){
        Optional<Portfolio> currPortfolio = portfolioRepo.findByUserIdAndLeagueId(userId, leagueId);
        if(currPortfolio.isEmpty()){
            return createPortfolio(userId, leagueId);
        } else {
            return currPortfolio.get();
        }
    }

    public Portfolio createPortfolio(Long userId, Long leagueId){
        Portfolio newPortfolio = new Portfolio();
        newPortfolio.setUserId(userId);
        newPortfolio.setLeagueId(leagueId);
        portfolioRepo.save(newPortfolio);
        return newPortfolio;
    }

    public void getPortfolioSummary(Long userId, Long leagueId){

    }

    public void processBuy(Long userId, Long leagueId, UUID stockId, int quantity, BigDecimal price){
        Portfolio currPortfolio = getPortfolio(userId, leagueId);
        holdingsService.addHolding(currPortfolio, stockId, quantity, price);
    }

    public void processSell(Long userId, Long leagueId, UUID stockId, int decreaseAmmount){
        Portfolio currPortfolio = getPortfolio(userId, leagueId);
        holdingsService.reduceHolding(currPortfolio, stockId, decreaseAmmount);
    }
}
