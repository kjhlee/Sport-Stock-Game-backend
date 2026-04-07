package com.sportstock.ingestion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportstock.ingestion.client.EspnFantasyClient;
import com.sportstock.ingestion.entity.Event;
import com.sportstock.ingestion.entity.EventCompetitor;
import com.sportstock.ingestion.entity.FantasySnapshot;
import com.sportstock.ingestion.entity.PlayerGameStat;
import com.sportstock.ingestion.entity.Team;
import com.sportstock.ingestion.mapper.PlayerGameStatsFantasyPointCalculator;
import com.sportstock.ingestion.repo.EventCompetitorRepository;
import com.sportstock.ingestion.repo.EventRepository;
import com.sportstock.ingestion.repo.FantasySnapshotRepository;
import com.sportstock.ingestion.repo.PlayerGameStatRepository;
import com.sportstock.ingestion.repo.TeamRosterEntryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FantasySnapshotIngestionServiceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Mock private EspnFantasyClient espnFantasyClient;
  @Mock private FantasySnapshotRepository fantasySnapshotRepository;
  @Mock private EventRepository eventRepository;
  @Mock private EventCompetitorRepository eventCompetitorRepository;
  @Mock private PlayerGameStatRepository playerGameStatRepository;
  @Mock private PlayerGameStatsFantasyPointCalculator calculator;
  @Mock private TeamRosterEntryRepository teamRosterEntryRepository;

  private FantasySnapshotIngestionService service;

  @BeforeEach
  void setUp() {
    service =
        new FantasySnapshotIngestionService(
            espnFantasyClient,
            fantasySnapshotRepository,
            eventRepository,
            eventCompetitorRepository,
            playerGameStatRepository,
            calculator,
            teamRosterEntryRepository);

    when(fantasySnapshotRepository.findByEventId(10L)).thenReturn(List.of());
    when(fantasySnapshotRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void ingestActualFantasyPoints_prefersCalculatorOverFantasyPayloadAppliedTotal() throws Exception {
    Event event = event();

    when(eventRepository.findByEspnId("401")).thenReturn(Optional.of(event));
    when(eventCompetitorRepository.findByEventEspnIdWithTeam("401")).thenReturn(competitors());
    when(playerGameStatRepository.findByEventId(10L)).thenReturn(List.of(playerGameStat("123")));
    when(espnFantasyClient.fetchPlayersByTeams(2026, 1, 2, List.of(1, 2)))
        .thenReturn(
            json(
                """
                [
                  {"id":123,"fullName":"Runner","defaultPositionId":2,
                   "stats":[{"statSourceId":0,"scoringPeriodId":1,"appliedTotal":8.25}]}
                ]
                """));
    when(calculator.computePlayerFantasyPoints(10L, "123")).thenReturn(bd("11.40"));

    var result = service.ingestActualFantasyPoints("401");

    assertEquals(1, result.updated());
    assertEquals(0, result.skipped());

    ArgumentCaptor<Iterable> captor = ArgumentCaptor.forClass(Iterable.class);
    verify(fantasySnapshotRepository).saveAll(captor.capture());
    FantasySnapshot snapshot = toList(captor.getValue()).get(0);
    assertEquals(bd("11.40"), snapshot.getActualFantasyPoints());
  }

  @Test
  void ingestActualFantasyPoints_fallsBackToPayloadAndSkipsUnsupportedDefenseNodes() throws Exception {
    Event event = event();

    when(eventRepository.findByEspnId("401")).thenReturn(Optional.of(event));
    when(eventCompetitorRepository.findByEventEspnIdWithTeam("401")).thenReturn(competitors());
    when(playerGameStatRepository.findByEventId(10L)).thenReturn(List.of(playerGameStat("123")));
    when(espnFantasyClient.fetchPlayersByTeams(2026, 1, 2, List.of(1, 2)))
        .thenReturn(
            json(
                """
                [
                  {"id":123,"fullName":"Receiver","defaultPositionId":3,
                   "stats":[{"statSourceId":0,"scoringPeriodId":1,"appliedTotal":6.50}]},
                  {"id":999,"fullName":"Chiefs D/ST","defaultPositionId":16,
                   "stats":[{"statSourceId":0,"scoringPeriodId":1,"appliedTotal":9.00}]}
                ]
                """));
    when(calculator.computePlayerFantasyPoints(10L, "123")).thenReturn(null);

    var result = service.ingestActualFantasyPoints("401");

    assertEquals(1, result.updated());
    assertEquals(1, result.skipped());

    ArgumentCaptor<Iterable> captor = ArgumentCaptor.forClass(Iterable.class);
    verify(fantasySnapshotRepository).saveAll(captor.capture());
    FantasySnapshot snapshot = toList(captor.getValue()).get(0);
    assertEquals("123", snapshot.getEspnId());
    assertEquals(bd("6.50"), snapshot.getActualFantasyPoints());
  }

  @Test
  void ingestProjections_mapsRealEspnPayloadFieldsAndSkipsTeamDefense() throws Exception {
    Event event = event();
    event.setStatusState("pre");

    when(eventRepository.findBySeasonYearAndSeasonTypeAndWeekNumberOrderByDateAsc(2026, 2, 1))
        .thenReturn(List.of(event));
    when(eventCompetitorRepository.findByEventEspnIdWithTeam("401")).thenReturn(competitors());
    when(espnFantasyClient.fetchPlayersByTeams(2026, 1, 2, List.of(1, 2)))
        .thenReturn(
            json(
                """
                [
                  {
                    "id": 3139477,
                    "fullName": "Patrick Mahomes",
                    "defaultPositionId": 1,
                    "proTeamId": 12,
                    "stats": [
                      {
                        "statSourceId": 1,
                        "scoringPeriodId": 1,
                        "stats": {
                          "0": 37.70793903,
                          "1": 25.01282024,
                          "3": 267.1316344,
                          "4": 2.03924968,
                          "19": 0.067008899,
                          "20": 0.797844429,
                          "23": 4.132280017,
                          "24": 21.5338189,
                          "25": 0.062464773,
                          "26": 0.003288992,
                          "62": 0.070297891,
                          "63": 0.00212,
                          "68": 0.482669998,
                          "72": 0.230772268
                        }
                      }
                    ]
                  },
                  {
                    "id": 999999,
                    "fullName": "Chiefs D/ST",
                    "defaultPositionId": 16,
                    "proTeamId": 12,
                    "stats": [
                      {
                        "statSourceId": 1,
                        "scoringPeriodId": 1,
                        "stats": {
                          "99": 3.0,
                          "105": 1.0,
                          "120": 17.0
                        }
                      }
                    ]
                  }
                ]
                """));

    var result = service.ingestProjections(2026, 2, 1);

    assertEquals(1, result.updated());
    assertEquals(1, result.skipped());

    ArgumentCaptor<Iterable> captor = ArgumentCaptor.forClass(Iterable.class);
    verify(fantasySnapshotRepository).saveAll(captor.capture());
    FantasySnapshot snapshot = toList(captor.getValue()).get(0);
    assertEquals("PLAYER", snapshot.getSubjectType());
    assertEquals("3139477", snapshot.getEspnId());
    assertEquals("Patrick Mahomes", snapshot.getFullName());
    assertEquals(bd("19.46"), snapshot.getProjectedFantasyPoints());
    assertEquals(
        "{\"passingAttempts\":37.70793903,\"passingCompletions\":25.01282024,\"passingYards\":267.1316344,\"passingTouchdowns\":2.03924968,\"passing2PtConversions\":0.067008899,\"passingInterceptions\":0.797844429,\"rushingAttempts\":4.132280017,\"rushingYards\":21.5338189,\"rushingTouchdowns\":0.062464773,\"rushing2PtConversions\":0.003288992,\"receptions\":0.00,\"receivingYards\":0.00,\"receivingTargets\":0.00,\"fumbleRecoveredForTD\":0.00212,\"fumbles\":0.482669998,\"lostFumbles\":0.230772268}",
        snapshot.getProjectedStats());
    verify(fantasySnapshotRepository, never()).deleteAll(any());
  }

  private static Event event() {
    Event event = new Event();
    event.setId(10L);
    event.setEspnId("401");
    event.setSeasonYear(2026);
    event.setSeasonType(2);
    event.setWeekNumber(1);
    event.setDate(Instant.now());
    return event;
  }

  private static List<EventCompetitor> competitors() {
    return List.of(competitor("1"), competitor("2"));
  }

  private static EventCompetitor competitor(String teamEspnId) {
    Team team = new Team();
    team.setEspnId(teamEspnId);
    team.setDisplayName("Team " + teamEspnId);
    EventCompetitor competitor = new EventCompetitor();
    competitor.setTeam(team);
    return competitor;
  }

  private static PlayerGameStat playerGameStat(String athleteEspnId) {
    PlayerGameStat stat = new PlayerGameStat();
    stat.setAthleteEspnId(athleteEspnId);
    return stat;
  }

  private static JsonNode json(String raw) throws Exception {
    return OBJECT_MAPPER.readTree(raw);
  }

  private static List<FantasySnapshot> toList(Iterable<?> iterable) {
    List<FantasySnapshot> snapshots = new ArrayList<>();
    for (Object item : iterable) {
      snapshots.add((FantasySnapshot) item);
    }
    return snapshots;
  }

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }
}
