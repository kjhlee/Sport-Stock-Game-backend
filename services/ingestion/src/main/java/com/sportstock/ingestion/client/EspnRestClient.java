package com.sportstock.ingestion.client;

import com.sportstock.ingestion.config.EspnApiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class EspnRestClient implements EspnApiClient {

    private final EspnApiProperties props;
    private final RestClient http;

    public EspnRestClient(EspnApiProperties props, @Qualifier("espnHttpClient") RestClient http) {
        this.props = props;
        this.http = http;
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
        return http.get()
                .uri(uri)
                .retrieve()
                .body(String.class);
    }
}
