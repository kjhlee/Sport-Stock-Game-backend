package com.sportstock.ingestion.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportstock.ingestion.client.EspnApiClient;
import com.sportstock.ingestion.entity.Athlete;
import com.sportstock.ingestion.entity.Team;
import com.sportstock.ingestion.entity.TeamRosterEntry;
import com.sportstock.ingestion.mapper.JsonPayloadCodec;
import com.sportstock.ingestion.repo.AthleteRepository;
import com.sportstock.ingestion.repo.CoachRepository;
import com.sportstock.ingestion.repo.TeamRepository;
import com.sportstock.ingestion.repo.TeamRosterEntryRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class RosterIngestionServiceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Mock private EspnApiClient espnApiClient;
  @Mock private TeamRepository teamRepository;
  @Mock private AthleteRepository athleteRepository;
  @Mock private TeamRosterEntryRepository teamRosterEntryRepository;
  @Mock private CoachRepository coachRepository;
  @Mock private JsonPayloadCodec jsonPayloadCodec;
  @Mock private TransactionTemplate transactionTemplate;
  @Mock private EntityManager entityManager;

  private RosterIngestionService service;
  private JsonNode rosterNode;

  @BeforeEach
  void setUp() throws Exception {
    service =
        new RosterIngestionService(
            espnApiClient,
            teamRepository,
            athleteRepository,
            teamRosterEntryRepository,
            coachRepository,
            jsonPayloadCodec,
            transactionTemplate,
            entityManager);
    rosterNode =
        OBJECT_MAPPER.readTree(
            """
                {
                  "athletes": [],
                  "coach": [],
                  "season": {"year": 2025}
                }
                """);
  }

  @Test
  void ingestAllRostersContinuesAfterPerTeamFailure() {
    runTransactionTemplateCallbacks();

    Team t1 = team("1");
    Team t2 = team("2");
    Team t3 = team("3");
    when(teamRepository.findAllByOrderByDisplayNameAsc()).thenReturn(List.of(t1, t2, t3));
    when(teamRepository.findByEspnId("1")).thenReturn(Optional.of(t1));
    when(teamRepository.findByEspnId("2")).thenReturn(Optional.of(t2));
    when(teamRepository.findByEspnId("3")).thenReturn(Optional.of(t3));
    when(espnApiClient.fetchTeamRoster("1")).thenReturn("roster-1");
    when(espnApiClient.fetchTeamRoster("3")).thenReturn("roster-3");
    when(espnApiClient.fetchTeamRoster("2")).thenThrow(new RuntimeException("boom"));
    when(jsonPayloadCodec.parseJson(anyString())).thenReturn(rosterNode);

    assertDoesNotThrow(() -> service.ingestAllRosters(2025, 100, null));

    verify(espnApiClient).fetchTeamRoster("1");
    verify(espnApiClient).fetchTeamRoster("2");
    verify(espnApiClient).fetchTeamRoster("3");
    verify(transactionTemplate, times(3)).executeWithoutResult(any());
  }

  @Test
  void ingestTeamRosterReloadsExistingAthleteWhenInsertHitsDuplicateKey() throws Exception {
    Team team = team("30");
    team.setId(30L);

    JsonNode duplicateRosterNode =
        OBJECT_MAPPER.readTree(
            """
                {
                  "athletes": [
                    {
                      "position": "QB",
                      "items": [
                        {
                          "id": "4911851",
                          "fullName": "Player Example",
                          "displayName": "Player Example"
                        }
                      ]
                    }
                  ],
                  "coach": [],
                  "season": {"year": 2025}
                }
                """);

    Athlete existingAthlete = new Athlete();
    existingAthlete.setId(99L);
    existingAthlete.setEspnId("4911851");
    existingAthlete.setFullName("4911851");

    when(teamRepository.findByEspnId("30")).thenReturn(Optional.of(team));
    when(espnApiClient.fetchTeamRoster("30")).thenReturn("roster-30");
    when(jsonPayloadCodec.parseJson(anyString())).thenReturn(duplicateRosterNode);
    when(athleteRepository.findByEspnId("4911851"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(existingAthlete));

    AtomicInteger saveCalls = new AtomicInteger();
    when(athleteRepository.save(any(Athlete.class)))
        .thenAnswer(
            invocation -> {
              Athlete athlete = invocation.getArgument(0);
              if (saveCalls.getAndIncrement() == 0) {
                throw new DataIntegrityViolationException("duplicate key");
              }
              return athlete;
            });

    when(teamRosterEntryRepository.findByTeamIdAndAthleteIdAndSeasonYear(30L, 99L, 2025))
        .thenReturn(Optional.empty());
    when(teamRosterEntryRepository.save(any(TeamRosterEntry.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    assertDoesNotThrow(() -> service.ingestTeamRoster("30", 2025, 200));

    verify(athleteRepository, times(2)).findByEspnId("4911851");
    verify(athleteRepository, times(2)).save(any(Athlete.class));
    verify(entityManager).detach(any(Athlete.class));
    verify(teamRosterEntryRepository).findByTeamIdAndAthleteIdAndSeasonYear(30L, 99L, 2025);
    verify(teamRosterEntryRepository).save(any(TeamRosterEntry.class));
  }

  private void runTransactionTemplateCallbacks() {
    doAnswer(
            invocation -> {
              Consumer<TransactionStatus> action = invocation.getArgument(0);
              action.accept(null);
              return null;
            })
        .when(transactionTemplate)
        .executeWithoutResult(any());
  }

  private Team team(String espnId) {
    Team team = new Team();
    team.setEspnId(espnId);
    return team;
  }
}
