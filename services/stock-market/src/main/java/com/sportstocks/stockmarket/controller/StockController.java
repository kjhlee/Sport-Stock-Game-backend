package com.sportstocks.stockmarket.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sportstocks.stockmarket.dto.response.SyncAthletesResponse;
import com.sportstocks.stockmarket.service.AthleteStockSyncService;

@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {

    private final AthleteStockSyncService athleteStockSyncService;

    public StockController(AthleteStockSyncService athleteStockSyncService) {
        this.athleteStockSyncService = athleteStockSyncService;
    }

    @PostMapping("/sync-athletes")
    public SyncAthletesResponse syncAthletes(
            @RequestParam(required = false) String position
    ) {
        AthleteStockSyncService.SyncAthletesResult result =
                athleteStockSyncService.syncAthletes(position);

        return new SyncAthletesResponse(
                result.created(),
                result.updated(),
                result.skipped(),
                result.totalFetched()
        );
    }
}