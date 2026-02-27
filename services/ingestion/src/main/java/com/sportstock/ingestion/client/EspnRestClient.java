package com.sportstock.ingestion.client;

import com.sportstock.ingestion.config.EspnApiProperties;
import com.sportstock.ingestion.exception.IngestionException;
import com.sportstock.ingestion.util.RateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class EspnRestClient implements EspnApiClient {

    private static final int MAX_ATTEMPTS = 4;

    private final EspnApiProperties props;
    private final RestClient http;
    private final RateLimiter rateLimiter;

    public EspnRestClient(EspnApiProperties props, @Qualifier("espnHttpClient") RestClient http, RateLimiter rateLimiter) {
        this.props = props;
        this.http = http;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public String fetchTeams() {
        URI uri = site()
                .pathSegment("sports", props.getSport(), props.getLeague(), "teams")
                .build()
                .encode()
                .toUri();
        return get(uri);
    }

    @Override
    public String fetchTeamDetail(String teamEspnId) {
        URI uri = site()
                .pathSegment("sports", props.getSport(), props.getLeague(), "teams", teamEspnId)
                .build()
                .encode()
                .toUri();
        return get(uri);
    }

    @Override
    public String fetchTeamRoster(String teamEspnId) {
        URI uri = site()
                .pathSegment("sports", props.getSport(), props.getLeague(), "teams", teamEspnId, "roster")
                .build()
                .encode()
                .toUri();
        return get(uri);
    }

    @Override
    public String fetchScoreboard(Integer seasonYear, Integer seasonType, Integer week) {
        URI uri = site()
                .pathSegment("sports", props.getSport(), props.getLeague(), "scoreboard")
                .queryParam("dates", seasonYear)
                .queryParam("seasontype", seasonType)
                .queryParam("week", week)
                .build()
                .encode()
                .toUri();
        return get(uri);
    }

    @Override
    public String fetchEventSummary(String eventEspnId) {
        URI uri = site()
                .pathSegment("sports", props.getSport(), props.getLeague(), "summary")
                .queryParam("event", eventEspnId)
                .build()
                .encode()
                .toUri();
        return get(uri);
    }

    @Override
    public String fetchAthletes(Integer pageSize, Integer page) {
        URI uri = core()
                .pathSegment("sports", props.getSport(), "leagues", props.getLeague(), "athletes")
                .queryParam("limit", pageSize)
                .queryParam("page", page)
                .build()
                .encode()
                .toUri();
        return get(uri);
    }

    private UriComponentsBuilder site() {
        return UriComponentsBuilder.fromUriString(props.getSiteBaseUrl());
    }

    private UriComponentsBuilder core() {
        return UriComponentsBuilder.fromUriString(props.getCoreBaseUrl());
    }

    private String get(URI uri) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            rateLimiter.acquirePermit();
            try {
                return http.get()
                        .uri(uri)
                        .retrieve()
                        .body(String.class);
            } catch (RestClientResponseException ex) {
                int status = ex.getStatusCode().value();
                boolean retryable = status == 429 || status >= 500;
                if (!retryable || attempt == MAX_ATTEMPTS) {
                    throw ex;
                }

                long backoffMs = 250L * (1L << (attempt - 1));
                sleep(backoffMs);
            }
        }

        throw new IngestionException("Failed ESPN call after retries: " + uri);
    }

    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IngestionException("Interrupted during ESPN retry backoff", e);
        }
    }
}
