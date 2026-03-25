package com.sportstock.transaction.client;

import com.sportstock.common.dto.stock_market.StockResponse;
import com.sportstock.common.security.RequestContextAuthorizationHeaderResolver;
import com.sportstock.transaction.exception.TransactionException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockMarketServiceClient {

  private final RestClient stockMarketRestClient;

  public StockResponse getStock(UUID stockId) {
    try {
      return stockMarketRestClient
          .get()
          .uri("/api/v1/stocks/{stockId}", stockId)
          .header(
              HttpHeaders.AUTHORIZATION,
              RequestContextAuthorizationHeaderResolver.resolveBearerAuthorizationHeader())
          .retrieve()
          .body(StockResponse.class);
    } catch (RestClientResponseException e) {
      log.error("Failed to fetch stock {}: {}", stockId, e.getMessage());
      throw new TransactionException("Stock market service unavailable", e);
    }
  }
}
