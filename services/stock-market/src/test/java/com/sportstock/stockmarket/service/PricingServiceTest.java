package com.sportstock.stockmarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

    service = new PricingService(ingestionApiClient, stockRepository, priceHistoryRepository, pricingConfig);

    when(stockRepository.save(any(Stock.class)))
        .thenAnswer(
            invocation -> {
              Stock stock = invocation.getArgument(0);
              if (stock.getId() == null) {
                stock.setId(UUID.randomUUID());
              }
              return stock;
            });
    when(priceHistoryRepository.save(any(PriceHistory.class)))
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

    when(stockRepository.findByStatusAndGameLockedFalse(StockStatus.ACTIVE)).thenReturn(List.of(stock));
    when(ingestionApiClient.getFantasySnapshot("3139477", "PLAYER", 2026, 2, 5))
        .thenReturn(
            new FantasySnapshotResponse(
                null, null, "PLAYER", "3139477", "Patrick Mahomes", null, bd("1.50"), null, false, null));
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
                    null, "401", "TEAM_DEFENSE", "12", "Chiefs D/ST", null, null, bd("9.00"), true, null)));
    when(ingestionApiClient.getEvent("401"))
        .thenReturn(new IngestionEventDto("401", 2026, 2, 5, true, "post"));
    when(stockRepository.findByEspnIdAndType("12", StockType.TEAM_DEFENSE)).thenReturn(Optional.empty());
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

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }
}
