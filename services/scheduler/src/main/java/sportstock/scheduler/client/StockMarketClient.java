package sportstock.scheduler.client;

import com.sportstock.common.dto.stock_market.PriceUpdateResponse;
import com.sportstock.common.dto.stock_market.SyncAthletesResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
public class StockMarketClient {

    private final RestClient restClient;

    public PriceUpdateResponse updateProjectedPrices(
            int seasonYear, int seasonType, int weekNumber) {
        return restClient
                .post()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/update-projected-prices")
                                        .queryParam("seasonYear", seasonYear)
                                        .queryParam("seasonType", seasonType)
                                        .queryParam("weekNumber", weekNumber)
                                        .build())
                .retrieve()
                .body(PriceUpdateResponse.class);
    }

    public PriceUpdateResponse updateFinalPrices(String eventEspnId) {
        return restClient
                .post()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/update-final-prices")
                                        .queryParam("eventEspnId", eventEspnId)
                                        .build())
                .retrieve()
                .body(PriceUpdateResponse.class);
    }

    public int lockEvent(String eventEspnId) {
        Map<String, Integer> body =
                restClient
                        .post()
                        .uri(
                                uriBuilder ->
                                        uriBuilder
                                                .path("/lock-event")
                                                .queryParam("eventEspnId", eventEspnId)
                                                .build())
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Integer>>() {});
        return body != null ? body.getOrDefault("locked", 0) : 0;
    }

    public int unlockAll() {
        Map<String, Integer> body =
                restClient
                        .post()
                        .uri("/unlock-all")
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Integer>>() {});
        return body != null ? body.getOrDefault("unlocked", 0) : 0;
    }

    public void syncInjuries(int seasonYear) {
        restClient
                .post()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path("/sync-injuries")
                                        .queryParam("seasonYear", seasonYear)
                                        .build())
                .retrieve()
                .toBodilessEntity();
        log.info("Synced injuries for season {}", seasonYear);
    }

    public SyncAthletesResponse syncAthletes(String position) {
        return restClient
                .post()
                .uri(
                        uriBuilder -> {
                            uriBuilder.path("/sync-athletes");
                            if (position != null && !position.isBlank()) {
                                uriBuilder.queryParam("position", position);
                            }
                            return uriBuilder.build();
                        })
                .retrieve()
                .body(SyncAthletesResponse.class);
    }

    public SyncAthletesResponse syncTeamDefenseStocks() {
        return restClient
                .post()
                .uri("/sync-team-defense")
                .retrieve()
                .body(SyncAthletesResponse.class);
    }
}
