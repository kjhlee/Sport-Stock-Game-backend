package com.sportstock.portfolio.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sportstock.common.dto.portfolio.ProcessBuyRequest;
import com.sportstock.common.dto.portfolio.ProcessSellRequest;
import com.sportstock.common.security.CurrentUserProvider;
import com.sportstock.portfolio.entity.Portfolio;
import com.sportstock.portfolio.service.PortfolioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private static final Logger log = LoggerFactory.getLogger(PortfolioController.class);

    private final PortfolioService portfolioService;
    private final CurrentUserProvider currentUserProvider;

    //Get a user's portfolio per that league
    @GetMapping("/{leagueId}")
    @ResponseStatus(HttpStatus.OK)
    public Portfolio getPortfolio(@PathVariable Long leagueId){
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("GET portfolio request: userId={} leagueId={}", userId, leagueId);
        return portfolioService.getPortfolio(userId, leagueId);
    }

    //Should be called by transaction service after a buy
    @PostMapping("/{leagueId}/buy")
    @ResponseStatus(HttpStatus.OK)
    public void processBuy(@RequestBody ProcessBuyRequest request, @PathVariable Long leagueId){
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("POST buy request: userId={} leagueId={} stockId={} quantity={} price={}", userId, leagueId, request.getStockId(), request.getQuantity(), request.getPrice());
        portfolioService.processBuy(userId, leagueId, request.getStockId(), request.getQuantity(), request.getPrice());
    }

    @PostMapping("/{leagueId}/sell")
    @ResponseStatus(HttpStatus.OK)
    public void processSell(@RequestBody ProcessSellRequest request, @PathVariable Long leagueId){
        Long userId = currentUserProvider.getCurrentUserId();
        log.info("POST sell request: userId={} leagueId={} stockId={} decreaseAmount={}", userId, leagueId, request.getStockId(), request.getDecreaseAmmount());
        portfolioService.processSell(userId, leagueId, request.getStockId(), request.getDecreaseAmmount());
    }

}
