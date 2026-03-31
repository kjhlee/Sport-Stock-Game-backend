package com.sportstock.portfolio.service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sportstock.portfolio.entity.Portfolio;
import com.sportstock.portfolio.repo.PortfolioRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    private final PortfolioRepo portfolioRepo;
    private final HoldingsService holdingsService;

    public Portfolio getPortfolio(Long userId, Long leagueId){
        log.info("Fetching portfolio for userId={} leagueId={}", userId, leagueId);
        Optional<Portfolio> currPortfolio = portfolioRepo.findByUserIdAndLeagueId(userId, leagueId);
        if(currPortfolio.isEmpty()){
            log.info("No portfolio found for userId={} leagueId={}, creating new one", userId, leagueId);
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
        log.info("Created portfolio id={} for userId={} leagueId={}", newPortfolio.getId(), userId, leagueId);
        return newPortfolio;
    }

    public void getPortfolioSummary(Long userId, Long leagueId){

    }

    public void processBuy(Long userId, Long leagueId, UUID stockId, int quantity, BigDecimal price){
        log.info("Processing buy: userId={} leagueId={} stockId={} quantity={} price={}", userId, leagueId, stockId, quantity, price);
        Portfolio currPortfolio = getPortfolio(userId, leagueId);
        holdingsService.addHolding(currPortfolio, stockId, quantity, price);
    }

    public void processSell(Long userId, Long leagueId, UUID stockId, int decreaseAmmount){
        log.info("Processing sell: userId={} leagueId={} stockId={} decreaseAmount={}", userId, leagueId, stockId, decreaseAmmount);
        Portfolio currPortfolio = getPortfolio(userId, leagueId);
        holdingsService.reduceHolding(currPortfolio, stockId, decreaseAmmount);
    }
}
