package com.sportstock.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportstock.ingestion.client.EspnApiClient;
import com.sportstock.ingestion.entity.Team;
import com.sportstock.ingestion.mapper.JsonPayloadCodec;
import com.sportstock.ingestion.repo.AthleteRepository;
import com.sportstock.ingestion.repo.CoachRepository;
import com.sportstock.ingestion.repo.TeamRepository;
import com.sportstock.ingestion.repo.TeamRosterEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RosterIngestionServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private EspnApiClient espnApiClient;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private AthleteRepository athleteRepository;
    @Mock
    private TeamRosterEntryRepository teamRosterEntryRepository;
    @Mock
    private CoachRepository coachRepository;
    @Mock
    private JsonPayloadCodec jsonPayloadCodec;
    @Mock
    private TransactionTemplate transactionTemplate;

    private RosterIngestionService service;
    private JsonNode rosterNode;

    @BeforeEach
    void setUp() throws Exception {
        service = new RosterIngestionService(
                espnApiClient,
                teamRepository,
                athleteRepository,
                teamRosterEntryRepository,
                coachRepository,
                jsonPayloadCodec,
                transactionTemplate
        );
        rosterNode = OBJECT_MAPPER.readTree("""
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

    private void runTransactionTemplateCallbacks() {
        doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    private Team team(String espnId) {
        Team team = new Team();
        team.setEspnId(espnId);
        return team;
    }
}
