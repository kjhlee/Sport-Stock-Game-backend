package com.sportstock.league.client;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionServiceClient {

  private final RestClient ingestionRestClient;

  public Boolean isSeasonActive() {
    try {
      SeasonActiveResponse response =
          ingestionRestClient
              .get()
              .uri("/api/v1/ingestion/seasons/season-active")
              .retrieve()
              .body(SeasonActiveResponse.class);

      return response != null && Boolean.TRUE.equals(response.active());

    } catch (Exception e) {
      log.error("Failed to check if season is active: {}", e.getMessage());
      throw new RuntimeException("Cannot determine if season active", e);
    }
  }

  public CurrentWeekResponse getCurrentWeekOrPreseasonOptional() {
    try {
      return ingestionRestClient
          .get()
          .uri("/api/v1/ingestion/seasons/current-week-or-preseason/optional")
          .retrieve()
          .body(CurrentWeekResponse.class);
    } catch (Exception e) {
      log.error("Failed to fetch current NFL week or preseason week: {}", e.getMessage());
      throw new RuntimeException("Cannot determine current NFL phase", e);
    }
  }

  public record SeasonActiveResponse(Boolean active) {}
}
