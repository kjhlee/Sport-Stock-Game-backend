package com.sportstock.ingestion.client;

public interface EspnApiClient {

    String fetchTeams();

    String fetchTeamDetail(String teamEspnId);

    String fetchTeamRoster(String teamEspnId);

    String fetchScoreboard(Integer seasonYear, Integer seasonType, Integer week);

    String fetchEventSummary(String eventEspnId);

    String fetchAthletes(Integer pageSize, Integer page);
}
