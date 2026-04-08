package com.sportstock.stockmarket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportstock.common.dto.stock_market.IngestionAthleteDto;
import com.sportstock.common.enums.stock_market.StockType;
import com.sportstock.stockmarket.client.IngestionApiClient;
import com.sportstock.stockmarket.config.PricingConfig;
import com.sportstock.stockmarket.model.entity.Stock;
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
class StockSyncServiceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Mock private IngestionApiClient ingestionApiClient;
  @Mock private StockRepository stockRepository;

  private StockSyncService service;

  @BeforeEach
  void setUp() {
    PricingConfig pricingConfig = new PricingConfig();
    pricingConfig.setBasePrices(Map.of("K", bd("5.00"), "QB", bd("15.00"), "DST", bd("8.00")));
    service = new StockSyncService(ingestionApiClient, stockRepository, pricingConfig);

    when(stockRepository.save(any(Stock.class)))
        .thenAnswer(
            invocation -> {
              Stock stock = invocation.getArgument(0);
              if (stock.getId() == null) {
                stock.setId(UUID.randomUUID());
              }
              return stock;
            });
  }

  @Test
  void syncAthletes_normalizesPkToKAndSkipsUnsupportedPositions() throws Exception {
    IngestionAthleteDto kicker =
        athlete(
            """
            {"espnId":"1","fullName":"Harrison Butker","positionAbbreviation":"PK","statusType":"ACTIVE","teamEspnId":"12"}
            """);
    IngestionAthleteDto unsupported =
        athlete(
            """
            {"espnId":"2","fullName":"Long Snapper","positionAbbreviation":"LS","statusType":"ACTIVE","teamEspnId":"12"}
            """);

    when(ingestionApiClient.getAthletes("PK")).thenReturn(List.of(kicker, unsupported));
    when(stockRepository.findByEspnIdAndType("1", StockType.PLAYER)).thenReturn(Optional.empty());

    var result = service.syncAthletes("k");

    assertEquals(1, result.created());
    assertEquals(0, result.updated());
    assertEquals(1, result.skipped());
    assertEquals(2, result.totalFetched());

    ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);
    verify(stockRepository).save(stockCaptor.capture());
    assertEquals("K", stockCaptor.getValue().getPosition());
    assertEquals(bd("5.00"), stockCaptor.getValue().getCurrentPrice());
  }

  @Test
  void syncAthletes_treatsNullStatusAsActive() throws Exception {
    IngestionAthleteDto athlete =
        athlete(
            """
            {"espnId":"1","fullName":"Player One","positionAbbreviation":"QB","teamEspnId":"12"}
            """);

    when(ingestionApiClient.getAthletes(null)).thenReturn(List.of(athlete));
    when(stockRepository.findByEspnIdAndType("1", StockType.PLAYER)).thenReturn(Optional.empty());

    service.syncAthletes(null);

    ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);
    verify(stockRepository).save(stockCaptor.capture());
    assertEquals(com.sportstock.common.enums.stock_market.StockStatus.ACTIVE,
        stockCaptor.getValue().getStatus());
  }

  @Test
  void syncAthletes_treatsBlankStatusAsActive() throws Exception {
    IngestionAthleteDto athlete =
        athlete(
            """
            {"espnId":"2","fullName":"Player Two","positionAbbreviation":"QB","statusType":"   ","teamEspnId":"12"}
            """);

    when(ingestionApiClient.getAthletes(null)).thenReturn(List.of(athlete));
    when(stockRepository.findByEspnIdAndType("2", StockType.PLAYER)).thenReturn(Optional.empty());

    service.syncAthletes(null);

    ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);
    verify(stockRepository).save(stockCaptor.capture());
    assertEquals(com.sportstock.common.enums.stock_market.StockStatus.ACTIVE,
        stockCaptor.getValue().getStatus());
  }

  @Test
  void syncAthletes_treatsInjuredReserveAsDelisted() throws Exception {
    IngestionAthleteDto athlete =
        athlete(
            """
            {"espnId":"3","fullName":"Player Three","positionAbbreviation":"QB","statusType":"INJURED_RESERVE","teamEspnId":"12"}
            """);

    when(ingestionApiClient.getAthletes(null)).thenReturn(List.of(athlete));
    when(stockRepository.findByEspnIdAndType("3", StockType.PLAYER)).thenReturn(Optional.empty());

    service.syncAthletes(null);

    ArgumentCaptor<Stock> stockCaptor = ArgumentCaptor.forClass(Stock.class);
    verify(stockRepository).save(stockCaptor.capture());
    assertEquals(com.sportstock.common.enums.stock_market.StockStatus.DELISTED,
        stockCaptor.getValue().getStatus());
  }

  private static IngestionAthleteDto athlete(String json) throws Exception {
    return OBJECT_MAPPER.readValue(json, IngestionAthleteDto.class);
  }

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }
}
