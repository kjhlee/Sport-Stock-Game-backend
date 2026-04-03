package com.sportstock.stockmarket.controller;

import com.sportstock.common.dto.stock_market.PagedStockResponse;
import com.sportstock.common.dto.stock_market.PriceHistoryResponse;
import com.sportstock.common.dto.stock_market.PriceUpdateResponse;
import com.sportstock.common.dto.stock_market.StockResponse;
import com.sportstock.common.dto.stock_market.SyncAthletesResponse;
import com.sportstock.common.enums.stock_market.StockStatus;
import com.sportstock.common.enums.stock_market.StockType;
import com.sportstock.stockmarket.service.StockLockService;
import com.sportstock.stockmarket.service.StockSyncService;
import com.sportstock.stockmarket.service.PricingService;
import com.sportstock.stockmarket.service.StockQueryService;
import java.util.List;
import java.util.UUID;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stocks")
public class StockController {

  private final StockQueryService stockQueryService;
  private final StockSyncService stockSyncService;
  private final PricingService pricingService;
  private final StockLockService stockLockService;

  public StockController(
      StockQueryService stockQueryService,
      StockSyncService stockSyncService,
      PricingService pricingService,
      StockLockService stockLockService) {
    this.stockQueryService = stockQueryService;
    this.stockSyncService = stockSyncService;
    this.pricingService = pricingService;
    this.stockLockService = stockLockService;
  }

  @GetMapping("/{stockId}")
  public StockResponse getStock(@PathVariable UUID stockId) {
    return stockQueryService.getStock(stockId);
  }

  @GetMapping("/espn/{espnId}")
  public StockResponse getStockByEspnId(
          @PathVariable String espnId,
          @RequestParam(defaultValue = "PLAYER") StockType type) {
    return stockQueryService.getStockByEspnId(espnId, type);
  }

  @GetMapping("/{stockId}/price-history")
  public List<PriceHistoryResponse> getPriceHistory(
      @PathVariable UUID stockId, @RequestParam int seasonYear, @RequestParam int seasonType) {
    return stockQueryService.getPriceHistory(stockId, seasonYear, seasonType);
  }

  @GetMapping
  public PagedStockResponse listStocks(
      @RequestParam(required = false) String position,
      @RequestParam(required = false) StockStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return stockQueryService.listStocks(position, status, page, size);
  }

  @PostMapping("/update-projected-prices")
  public PriceUpdateResponse updateProjectedPrices(
          @RequestParam int seasonYear, @RequestParam int seasonType, @RequestParam int weekNumber) {
    PricingService.PriceUpdateResult result =
            pricingService.updateProjectedPrices(seasonYear, seasonType, weekNumber);
    return new PriceUpdateResponse(result.updated(), result.skipped());
  }

  @PostMapping("/update-final-prices")
  public PriceUpdateResponse updateFinalPrices(@RequestParam String eventEspnId) {
    PricingService.PriceUpdateResult result =
            pricingService.updateFinalPrices(eventEspnId);
    return new PriceUpdateResponse(result.updated(), result.skipped());
  }

  @PostMapping("/sync-team-defense")
  public SyncAthletesResponse syncTeamDefenseStocks() {
    StockSyncService.SyncAthletesResult result = stockSyncService.syncTeamDefenseStocks();
    return new SyncAthletesResponse(result.created(), result.updated(), result.skipped(), result.totalFetched());
  }

  @PostMapping("/sync-athletes")
  public SyncAthletesResponse syncAthletes(@RequestParam(required = false) String position) {
    StockSyncService.SyncAthletesResult result =
        stockSyncService.syncAthletes(position);

    return new SyncAthletesResponse(
        result.created(), result.updated(), result.skipped(), result.totalFetched());
  }

  @PostMapping("/lock-event")
  public Map<String, Integer> lockEvent(@RequestParam String eventEspnId) {
    int locked = stockLockService.lockPlayersForEvent(eventEspnId);
    return Map.of("locked", locked);
  }

  @PostMapping("/unlock-all")
  public Map<String, Integer> unlockAll() {
    int unlocked = stockLockService.unlockAllForWeek();
    return Map.of("unlocked", unlocked);
  }

  @PostMapping("/sync-injuries")
  public StockLockService.InjurySyncResult syncInjuries() {
    return stockLockService.syncInjuryStatuses();
  }
}
