package com.sportstock.portfolio.service;

import org.springframework.stereotype.Service;

import com.sportstock.portfolio.repo.HoldingsRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HoldingsService {
    private final HoldingsRepo holdingsRepo;

    
}
