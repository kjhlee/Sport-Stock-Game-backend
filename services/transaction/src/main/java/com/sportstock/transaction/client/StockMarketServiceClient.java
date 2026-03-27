package com.sportstock.transaction.client;

import com.sportstock.common.dto.stock_market.StockResponse;
import com.sportstock.common.exceptions.MissingAuthenticationException;
import com.sportstock.transaction.exception.TransactionException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockMarketServiceClient {

  private final RestClient stockMarketRestClient;

  private String getAuthorizationHeader() {
    ServletRequestAttributes attrs =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs == null) {
      throw new MissingAuthenticationException("No request context available");
    }
    if (attrs.getRequest() == null) {
      throw new MissingAuthenticationException("No servlet request available");
    }
    String authorizationHeader = attrs.getRequest().getHeader("Authorization");
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      throw new MissingAuthenticationException("Missing Authorization header on incoming request");
    }
    if (!authorizationHeader.startsWith("Bearer ")) {
      throw new MissingAuthenticationException(
          "Authorization header must use Bearer token format (Authorization: Bearer <token>)");
    }
    return authorizationHeader;
  }

  public StockResponse getStock(UUID stockId) {
    try {
      return stockMarketRestClient
          .get()
          .uri("/api/v1/stocks/{stockId}", stockId)
          .header("Authorization", getAuthorizationHeader())
          .retrieve()
          .body(StockResponse.class);
    } catch (RestClientResponseException e) {
      log.error("Failed to fetch stock {}: {}", stockId, e.getMessage());
      throw new TransactionException("Stock market service unavailable", e);
    }
  }
}
