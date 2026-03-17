package com.sportstocks.stockmarket.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.sportstocks.stockmarket.dto.response.PagedStockResponse;
import com.sportstocks.stockmarket.dto.response.PriceHistoryResponse;
import com.sportstocks.stockmarket.dto.response.PriceUpdateResponse;
import com.sportstocks.stockmarket.dto.response.StockResponse;
import com.sportstocks.stockmarket.dto.response.SyncAthletesResponse;
import com.sportstocks.stockmarket.model.enums.StockStatus;
import com.sportstocks.stockmarket.service.AthleteStockSyncService;
import com.sportstocks.stockmarket.service.PricingService;
import com.sportstocks.stockmarket.service.StockQueryService;

@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {

    private final StockQueryService stockQueryService;
    private final AthleteStockSyncService athleteStockSyncService;
    private final PricingService pricingService;

    public StockController(
            StockQueryService stockQueryService,
            AthleteStockSyncService athleteStockSyncService,
            PricingService pricingService
    ) {
        this.stockQueryService = stockQueryService;
        this.athleteStockSyncService = athleteStockSyncService;
        this.pricingService = pricingService;
    }

    @GetMapping("/{stockId}")
    public StockResponse getStock(@PathVariable UUID stockId) {
        return stockQueryService.getStock(stockId);
    }

    @GetMapping("athlete/{espnAthleteId}")
    public StockResponse getStockByAthlete(@PathVariable String espnAthleteId) {
        return stockQueryService.getStockByEspnId(espnAthleteId);
    }
    @GetMapping("/{stockId}/price-history")
    public List<PriceHistoryResponse> getPriceHistory(
            @PathVariable UUID stockId,
            @RequestParam int seasonYear,
            @RequestParam int seasonType
    ) {
        return stockQueryService.getPriceHistory(stockId, seasonYear, seasonType);
    }

    @GetMapping
    public PagedStockResponse listStocks(
            @RequestParam(required = false) String position,
            @RequestParam(required = false) StockStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return stockQueryService.listStocks(position, status, page, size);
    }

    @PostMapping("/update-prices")
    public PriceUpdateResponse updatePrices(
            @RequestParam int seasonYear,
            @RequestParam int seasonType,
            @RequestParam int weekNumber
    ) {
        PricingService.PriceUpdateResult result = pricingService.updatePricesForWeek(seasonYear, seasonType, weekNumber);
        return new PriceUpdateResponse(result.updated(), result.skipped());
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