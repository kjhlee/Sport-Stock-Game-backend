package com.sportstock.transaction.client;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import com.sportstock.transaction.exception.TransactionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionServiceClient {

  private final RestClient ingestionRestClient;

  public CurrentWeekResponse getCurrentWeek() {
    try {
      return ingestionRestClient
          .get()
          .uri("/api/internal/ingestion/seasons/current-week")
          .retrieve()
          .body(CurrentWeekResponse.class);
    } catch (RestClientResponseException e) {
      log.error("Failed to fetch current week: {}", e.getMessage());
      throw new TransactionException("Ingestion service unavailable", e);
    }
  }
}
