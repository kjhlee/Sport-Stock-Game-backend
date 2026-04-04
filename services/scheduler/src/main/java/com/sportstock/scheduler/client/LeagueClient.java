package com.sportstock.scheduler.client;

import com.sportstock.common.dto.league.LeagueResponse;
import com.sportstock.common.dto.league.UpdateInitialStipendStatusRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
public class LeagueClient {

    private final RestClient restClient;

    public List<LeagueResponse> getActiveLeagues() {
        List<LeagueResponse> body =
                restClient
                        .get()
                        .uri("/active")
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<LeagueResponse>>() {});
        return body != null ? body : List.of();
    }

    public List<LeagueResponse> getPendingInitialStipendLeagues() {
        List<LeagueResponse> body =
                restClient
                        .get()
                        .uri("/pending-initial-stipend")
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<LeagueResponse>>() {});
        return body != null ? body : List.of();
    }

    public void updateInitialStipendStatus(Long leagueId, String status) {
        restClient
                .put()
                .uri("/{leagueId}/initial-stipend-status", leagueId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateInitialStipendStatusRequest(status))
                .retrieve()
                .toBodilessEntity();
        log.info("Updated initial stipend status for league {} to {}", leagueId, status);
    }

    public List<Long> getMemberUserIds(Long leagueId) {
        List<Long> body =
                restClient
                        .get()
                        .uri("/{leagueId}/member-ids", leagueId)
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<Long>>() {});
        return body != null ? body : List.of();
    }
}
