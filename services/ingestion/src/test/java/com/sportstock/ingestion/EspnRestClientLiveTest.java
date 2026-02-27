package com.sportstock.ingestion;

import com.sportstock.ingestion.client.EspnRestClient;
import com.sportstock.ingestion.config.EspnApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live smoke tests that hit the real ESPN API.
 * No Spring context or database required — constructs EspnRestClient directly.
 *
 * Run all live tests:
 *   ./mvnw test -pl services/ingestion -Dgroups=live -Dsurefire.failIfNoSpecifiedTests=false
 *
 * Run a single test:
 *   ./mvnw test -pl services/ingestion -Dgroups=live -Dtest=EspnRestClientLiveTest#fetchTeams_returnsJsonWithTeams
 */
@Tag("live")
class EspnRestClientLiveTest {

    private EspnRestClient client;

    @BeforeEach
    void setUp() {
        EspnApiProperties props = new EspnApiProperties();
        props.setSiteBaseUrl("https://site.api.espn.com/apis/site/v2");
        props.setCoreBaseUrl("https://sports.core.api.espn.com/v2");
        props.setSport("football");
        props.setLeague("nfl");
        props.setDefaultAthletePageSize(100);
        props.setDefaultRosterLimit(200);
        props.setRateLimitDelayMs(200);

        RestClient http = RestClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "SportStock-Ingestion/1.0")
                .build();

        client = new EspnRestClient(props, http);
    }

    @Test
    void fetchTeams_returnsJsonWithTeams() {
        String json = client.fetchTeams();
        System.out.println("=== fetchTeams (first 500 chars) ===\n" + truncate(json));
        assertThat(json).isNotBlank().contains("\"teams\"");
    }

    @Test
    void fetchTeamDetail_returnsJsonForTeam() {
        String json = client.fetchTeamDetail("22"); // Arizona Cardinals
        System.out.println("=== fetchTeamDetail (first 500 chars) ===\n" + truncate(json));
        assertThat(json).isNotBlank().contains("\"team\"");
    }

    @Test
    void fetchTeamRoster_returnsJsonWithAthletes() {
        String json = client.fetchTeamRoster("22"); // Arizona Cardinals
        System.out.println("=== fetchTeamRoster (first 500 chars) ===\n" + truncate(json));
        assertThat(json).isNotBlank().contains("\"athletes\"");
    }

    @Test
    void fetchScoreboard_returnsJsonWithEvents() {
        String json = client.fetchScoreboard(2024, 2, 1); // 2024 regular season week 1
        System.out.println("=== fetchScoreboard (first 500 chars) ===\n" + truncate(json));
        assertThat(json).isNotBlank().contains("\"events\"");
    }

    @Test
    void fetchEventSummary_returnsJsonWithBoxscore() {
        String json = client.fetchEventSummary("401671716"); // known 2024 regular season game
        System.out.println("=== fetchEventSummary (first 500 chars) ===\n" + truncate(json));
        assertThat(json).isNotBlank().contains("\"boxscore\"");
    }

    @Test
    void fetchAthletes_returnsJsonWithItems() {
        String json = client.fetchAthletes(10, 1);
        System.out.println("=== fetchAthletes (first 500 chars) ===\n" + truncate(json));
        assertThat(json).isNotBlank().contains("\"items\"");
    }

    private String truncate(String s) {
        return s.substring(0, Math.min(500, s.length()));
    }
}
