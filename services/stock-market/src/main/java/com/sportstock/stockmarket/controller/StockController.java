package com.sportstock.stockmarket.controller;

import com.sportstock.common.dto.stock_market.DelistResponse;
import com.sportstock.common.dto.stock_market.PagedStockResponse;
import com.sportstock.common.dto.stock_market.PriceHistoryResponse;
import com.sportstock.common.dto.stock_market.PriceUpdateResponse;
import com.sportstock.common.dto.stock_market.RelistResponse;
import com.sportstock.common.dto.stock_market.StockResponse;
import com.sportstock.common.dto.stock_market.SyncAthletesResponse;
import com.sportstock.common.enums.stock_market.StockStatus;
import com.sportstock.common.enums.stock_market.StockType;
import com.sportstock.stockmarket.service.PricingService;
import com.sportstock.stockmarket.service.StockLockService;
import com.sportstock.stockmarket.service.StockQueryService;
import com.sportstock.stockmarket.service.StockSyncService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api")
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

  @GetMapping({"/v1/stocks/{stockId}", "/internal/stocks/{stockId}"})
  public StockResponse getStock(@PathVariable UUID stockId) {
    return stockQueryService.getStock(stockId);
  }

  @GetMapping({"/v1/stocks/espn/{espnId}", "/internal/stocks/espn/{espnId}"})
  public StockResponse getStockByEspnId(
      @PathVariable String espnId, @RequestParam(defaultValue = "PLAYER") StockType type) {
    return stockQueryService.getStockByEspnId(espnId, type);
  }

  @GetMapping({"/v1/stocks/{stockId}/price-history", "/internal/stocks/{stockId}/price-history"})
  public List<PriceHistoryResponse> getPriceHistory(
      @PathVariable UUID stockId,
      @RequestParam @Min(2000) int seasonYear,
      @RequestParam @Min(1) int seasonType) {
    return stockQueryService.getPriceHistory(stockId, seasonYear, seasonType);
  }

  @GetMapping({"/v1/stocks", "/internal/stocks"})
  public PagedStockResponse listStocks(
      @RequestParam(required = false) String position,
      @RequestParam(required = false) StockStatus status,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Positive int size) {
    return stockQueryService.listStocks(position, status, page, size);
  }

  @PostMapping("/internal/stocks/update-projected-prices")
  public PriceUpdateResponse updateProjectedPrices(
      @RequestParam int seasonYear, @RequestParam int seasonType, @RequestParam int weekNumber) {
    PricingService.PriceUpdateResult result =
        pricingService.updateProjectedPrices(seasonYear, seasonType, weekNumber);
    return new PriceUpdateResponse(result.updated(), result.skipped());
  }

  @PostMapping("/internal/stocks/update-final-prices")
  public PriceUpdateResponse updateFinalPrices(@RequestParam String eventEspnId) {
    PricingService.PriceUpdateResult result = pricingService.updateFinalPrices(eventEspnId);
    return new PriceUpdateResponse(result.updated(), result.skipped());
  }

  @PostMapping("/internal/stocks/sync-team-defense")
  public SyncAthletesResponse syncTeamDefenseStocks() {
    StockSyncService.SyncAthletesResult result = stockSyncService.syncTeamDefenseStocks();
    return new SyncAthletesResponse(
        result.created(), result.updated(), result.skipped(), result.totalFetched());
  }

  @PostMapping("/internal/stocks/sync-athletes")
  public SyncAthletesResponse syncAthletes(@RequestParam(required = false) String position) {
    StockSyncService.SyncAthletesResult result = stockSyncService.syncAthletes(position);

    return new SyncAthletesResponse(
        result.created(), result.updated(), result.skipped(), result.totalFetched());
  }

  @PostMapping("/internal/stocks/lock-event")
  public Map<String, Integer> lockEvent(@RequestParam String eventEspnId) {
    int locked = stockLockService.lockPlayersForEvent(eventEspnId);
    return Map.of("locked", locked);
  }

  @PostMapping("/internal/stocks/unlock-all")
  public Map<String, Integer> unlockAll() {
    int unlocked = stockLockService.unlockAllForWeek();
    return Map.of("unlocked", unlocked);
  }

  @PostMapping("/internal/stocks/sync-injuries")
  public StockLockService.InjurySyncResult syncInjuries(@RequestParam int seasonYear) {
    return stockLockService.syncInjuryStatuses(seasonYear);
  }

  @PostMapping("/internal/stocks/delist-unprojected")
  public DelistResponse delistUnprojectedStocks(
      @RequestParam int seasonYear, @RequestParam int seasonType, @RequestParam int weekNumber) {
    PricingService.DelistResult result =
        pricingService.delistUnprojectedStocks(seasonYear, seasonType, weekNumber);
    return new DelistResponse(result.delisted(), result.kept());
  }

  @PostMapping("/internal/stocks/relist-projected")
  public RelistResponse relistProjectedStocks(
      @RequestParam int seasonYear, @RequestParam int seasonType, @RequestParam int weekNumber) {
    PricingService.RelistResult result =
        pricingService.relistProjectedStocks(seasonYear, seasonType, weekNumber);
    return new RelistResponse(result.relisted(), result.kept());
  }
}
