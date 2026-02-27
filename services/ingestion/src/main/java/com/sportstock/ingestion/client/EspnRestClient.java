package com.sportstock.ingestion.client;

import com.sportstock.ingestion.config.EspnApiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
        String url = "%s/sports/%s/%s/teams".formatted(props.getSiteBaseUrl(), props.getSport(), props.getLeague());
        return get(url);
    }

    @Override
    public String fetchTeamDetail(String teamEspnId) {
        String url = "%s/sports/%s/%s/teams/%s".formatted(props.getSiteBaseUrl(), props.getSport(), props.getLeague(), teamEspnId);
        return get(url);
    }

    @Override
    public String fetchTeamRoster(String teamEspnId) {
        String url = "%s/sports/%s/%s/teams/%s/roster".formatted(props.getSiteBaseUrl(), props.getSport(), props.getLeague(), teamEspnId);
        return get(url);
    }

    @Override
    public String fetchScoreboard(Integer seasonYear, Integer seasonType, Integer week) {
        String url = "%s/sports/%s/%s/scoreboard?dates=%d&seasontype=%d&week=%d".formatted(props.getSiteBaseUrl(), props.getSport(), props.getLeague(), seasonYear, seasonType, week);
        return get(url);
    }

    @Override
    public String fetchEventSummary(String eventEspnId) {
        String url = "%s/sports/%s/%s/summary?event=%s".formatted(props.getSiteBaseUrl(), props.getSport(), props.getLeague(), eventEspnId);
        return get(url);
    }

    @Override
    public String fetchAthletes(Integer pageSize, Integer page) {
        String url = "%s/sports/%s/leagues/%s/athletes?limit=%d&page=%d".formatted(props.getCoreBaseUrl(), props.getSport(), props.getLeague(), pageSize, page);
        return get(url);
    }

    private String get(String url) {
        return http.get()
                .uri(url)
                .retrieve()
                .body(String.class);
    }
}
