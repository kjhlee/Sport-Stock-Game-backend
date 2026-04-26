package com.sportstock.stockmarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportstock.common.dto.ingestion.FantasySnapshotResponse;
import com.sportstock.common.dto.stock_market.IngestionEventDto;
import com.sportstock.common.enums.stock_market.PriceType;
import com.sportstock.common.enums.stock_market.StockStatus;
import com.sportstock.common.enums.stock_market.StockType;
import com.sportstock.stockmarket.client.IngestionApiClient;
import com.sportstock.stockmarket.config.PricingConfig;
import com.sportstock.stockmarket.model.entity.PriceHistory;
import com.sportstock.stockmarket.model.entity.Stock;
import com.sportstock.stockmarket.repository.PriceHistoryRepository;
import com.sportstock.stockmarket.repository.StockRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

  @Mock private IngestionApiClient ingestionApiClient;
  @Mock private StockRepository stockRepository;
  @Mock private PriceHistoryRepository priceHistoryRepository;

  private PricingService service;

  @BeforeEach
  void setUp() {
    PricingConfig pricingConfig = new PricingConfig();
    pricingConfig.setPriceFloor(bd("1.00"));
    pricingConfig.setMultipliers(Map.of("QB", bd("0.5"), "DST", bd("1.0")));

    service =
        new PricingService(
            ingestionApiClient, stockRepository, priceHistoryRepository, pricingConfig);

    lenient()
        .when(stockRepository.save(any(Stock.class)))
        .thenAnswer(
            invocation -> {
              Stock stock = invocation.getArgument(0);
              if (stock.getId() == null) {
                stock.setId(UUID.randomUUID());
              }
              return stock;
            });
    lenient()
        .when(priceHistoryRepository.save(any(PriceHistory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void updateProjectedPrices_appliesConfiguredPriceFloor() {
    Stock stock = new Stock();
    stock.setId(UUID.randomUUID());
    stock.setEspnId("3139477");
    stock.setType(StockType.PLAYER);
    stock.setPosition("QB");
    stock.setStatus(StockStatus.ACTIVE);

    when(stockRepository.findByStatusAndGameLockedFalse(StockStatus.ACTIVE))
        .thenReturn(List.of(stock));
    when(ingestionApiClient.getFantasySnapshot("3139477", "PLAYER", 2026, 2, 5))
        .thenReturn(
            new FantasySnapshotResponse(
                null,
                null,
                "PLAYER",
                "3139477",
                "Patrick Mahomes",
                null,
                bd("1.50"),
                null,
                false,
                null));
    when(priceHistoryRepository.findByStockIdAndSeasonYearAndSeasonTypeAndWeekAndPriceType(
            stock.getId(), 2026, 2, 5, PriceType.BASE))
        .thenReturn(Optional.empty());

    var result = service.updateProjectedPrices(2026, 2, 5);

    assertEquals(1, result.updated());
    assertEquals(0, result.skipped());
    assertEquals(bd("1.00"), stock.getCurrentPrice());

    ArgumentCaptor<PriceHistory> historyCaptor = ArgumentCaptor.forClass(PriceHistory.class);
    verify(priceHistoryRepository).save(historyCaptor.capture());
    assertEquals(PriceType.BASE, historyCaptor.getValue().getPriceType());
    assertEquals(bd("1.00"), historyCaptor.getValue().getPrice());
  }

  @Test
  void updateFinalPrices_createsMissingDefenseStockAndWritesFinalHistory() {
    when(ingestionApiClient.getFantasySnapshotsByEvent("401"))
        .thenReturn(
            List.of(
                new FantasySnapshotResponse(
                    null,
                    "401",
                    "TEAM_DEFENSE",
                    "12",
                    "Chiefs D/ST",
                    null,
                    null,
                    bd("9.00"),
                    true,
                    null)));
    when(ingestionApiClient.getEvent("401"))
        .thenReturn(new IngestionEventDto("401", 2026, 2, 5, true, "post"));
    when(stockRepository.findByEspnIdAndType("12", StockType.TEAM_DEFENSE))
        .thenReturn(Optional.empty());
    when(priceHistoryRepository.findByStockIdAndSeasonYearAndSeasonTypeAndWeekAndPriceType(
            any(), any(Integer.class), any(Integer.class), any(Integer.class), any()))
        .thenReturn(Optional.empty());

    var result = service.updateFinalPrices("401");

    assertEquals(1, result.updated());
    assertEquals(0, result.skipped());

    ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);
    verify(stockRepository, times(2)).save(stockCaptor.capture());
    Stock created = stockCaptor.getAllValues().get(0);
    Stock repriced = stockCaptor.getAllValues().get(1);

    assertEquals("DST", created.getPosition());
    assertEquals(StockStatus.DELISTED, created.getStatus());
    assertEquals(bd("9.00"), repriced.getCurrentPrice());
  }

  @Test
  void delistUnprojectedStocks_delistsStocksWithNoProjection() {
    Stock withProjection = new Stock();
    withProjection.setId(UUID.randomUUID());
    withProjection.setEspnId("100");
    withProjection.setType(StockType.PLAYER);
    withProjection.setPosition("QB");
    withProjection.setStatus(StockStatus.ACTIVE);

    Stock withoutProjection = new Stock();
    withoutProjection.setId(UUID.randomUUID());
    withoutProjection.setEspnId("200");
    withoutProjection.setType(StockType.PLAYER);
    withoutProjection.setPosition("RB");
    withoutProjection.setStatus(StockStatus.ACTIVE);

    when(stockRepository.findByStatusAndGameLockedFalse(StockStatus.ACTIVE))
        .thenReturn(List.of(withProjection, withoutProjection));
    when(ingestionApiClient.getFantasySnapshot("100", "PLAYER", 2026, 2, 6))
        .thenReturn(
            new FantasySnapshotResponse(
                null, null, "PLAYER", "100", "Player A", null, bd("15.00"), null, false, null));
    when(ingestionApiClient.getFantasySnapshot("200", "PLAYER", 2026, 2, 6)).thenReturn(null);

    var result = service.delistUnprojectedStocks(2026, 2, 6);

    assertEquals(1, result.delisted());
    assertEquals(1, result.kept());
    assertEquals(StockStatus.ACTIVE, withProjection.getStatus());
    assertEquals(StockStatus.DELISTED, withoutProjection.getStatus());
  }

  @Test
  void delistUnprojectedStocks_delistsWhenProjectionPointsAreNull() {
    Stock stock = new Stock();
    stock.setId(UUID.randomUUID());
    stock.setEspnId("300");
    stock.setType(StockType.PLAYER);
    stock.setPosition("WR");
    stock.setStatus(StockStatus.ACTIVE);

    when(stockRepository.findByStatusAndGameLockedFalse(StockStatus.ACTIVE))
        .thenReturn(List.of(stock));
    when(ingestionApiClient.getFantasySnapshot("300", "PLAYER", 2026, 2, 6))
        .thenReturn(
            new FantasySnapshotResponse(
                null, null, "PLAYER", "300", "Player B", null, null, null, false, null));

    var result = service.delistUnprojectedStocks(2026, 2, 6);

    assertEquals(1, result.delisted());
    assertEquals(0, result.kept());
    assertEquals(StockStatus.DELISTED, stock.getStatus());
  }

  @Test
  void delistUnprojectedStocks_skipsGameLockedStocks() {
    when(stockRepository.findByStatusAndGameLockedFalse(StockStatus.ACTIVE)).thenReturn(List.of());

    var result = service.delistUnprojectedStocks(2025, 2, 5);

    assertEquals(0, result.delisted());
    assertEquals(0, result.kept());
  }

  @Test
  void relistProjectedStocks_relistsAndPricesDelistedStocksWithProjections() {
    Stock delistedWithProjection = new Stock();
    delistedWithProjection.setId(UUID.randomUUID());
    delistedWithProjection.setEspnId("400");
    delistedWithProjection.setType(StockType.PLAYER);
    delistedWithProjection.setPosition("QB");
    delistedWithProjection.setStatus(StockStatus.DELISTED);
    delistedWithProjection.setCurrentPrice(bd("5.00"));

    Stock delistedWithoutProjection = new Stock();
    delistedWithoutProjection.setId(UUID.randomUUID());
    delistedWithoutProjection.setEspnId("500");
    delistedWithoutProjection.setType(StockType.PLAYER);
    delistedWithoutProjection.setPosition("RB");
    delistedWithoutProjection.setStatus(StockStatus.DELISTED);

    when(stockRepository.findByStatus(StockStatus.DELISTED))
        .thenReturn(List.of(delistedWithProjection, delistedWithoutProjection));
    when(ingestionApiClient.getFantasySnapshot("400", "PLAYER", 2026, 2, 6))
        .thenReturn(
            new FantasySnapshotResponse(
                null, null, "PLAYER", "400", "Player C", null, bd("20.00"), null, false, null));
    when(ingestionApiClient.getFantasySnapshot("500", "PLAYER", 2026, 2, 6)).thenReturn(null);
    when(priceHistoryRepository.findByStockIdAndSeasonYearAndSeasonTypeAndWeekAndPriceType(
            delistedWithProjection.getId(), 2026, 2, 6, PriceType.BASE))
        .thenReturn(Optional.empty());

    var result = service.relistProjectedStocks(2026, 2, 6);

    assertEquals(1, result.relisted());
    assertEquals(1, result.kept());
    assertEquals(StockStatus.ACTIVE, delistedWithProjection.getStatus());
    assertEquals(bd("10.00"), delistedWithProjection.getCurrentPrice());
    assertEquals(StockStatus.DELISTED, delistedWithoutProjection.getStatus());
  }

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }
}
