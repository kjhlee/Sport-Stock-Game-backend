package com.sportstock.scheduler.client;

import com.sportstock.common.dto.transaction.IssueStipendRequest;
import com.sportstock.common.dto.transaction.StipendResultResponse;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
public class TransactionClient {

    private final RestClient restClient;

    public StipendResultResponse issueWeeklyStipends(
            Long leagueId, BigDecimal amount, int weekNumber) {
        IssueStipendRequest request = new IssueStipendRequest(leagueId, amount, null);
        return restClient
                .post()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/stipends/weekly")
                                        .queryParam("weekNumber", weekNumber)
                                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(StipendResultResponse.class);
    }

    public void liquidateAssets(Long leagueId, int weekNumber) {
        restClient
                .post()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/liquidate")
                                        .queryParam("leagueId", leagueId)
                                        .queryParam("weekNumber", weekNumber)
                                        .build())
                .retrieve()
                .toBodilessEntity();
        log.info("Liquidated assets for league {} week {}", leagueId, weekNumber);
    }

    public StipendResultResponse issueInitialStipends(
            Long leagueId, BigDecimal amount, List<Long> userIds) {
        IssueStipendRequest request = new IssueStipendRequest(leagueId, amount, userIds);
        return restClient
                .post()
                .uri("/stipends/initial")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(StipendResultResponse.class);
    }
}
