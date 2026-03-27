package com.sportstock.portfolio.service;

import org.springframework.stereotype.Service;

import com.sportstock.portfolio.repo.PortfolioRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PortfolioService {
    private final PortfolioRepo portfolioRepo;
}
