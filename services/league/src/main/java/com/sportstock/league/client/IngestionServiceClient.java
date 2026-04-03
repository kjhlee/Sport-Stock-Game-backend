package com.sportstock.league.client;

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
            return ingestionRestClient
                    .post()
                    .uri("/api/v1/ingestion/seasons/season-active")
                    .retrieve()
                    .body(Boolean.class);
        } catch (Exception e) {
            log.error("Failed to check if season is active: {}", e.getMessage());
            throw new RuntimeException("Cannot determine if season active", e);
        }
    }
}
