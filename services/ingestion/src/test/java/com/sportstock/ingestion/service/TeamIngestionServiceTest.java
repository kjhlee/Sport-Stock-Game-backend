package com.sportstock.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportstock.ingestion.client.EspnApiClient;
import com.sportstock.ingestion.entity.Team;
import com.sportstock.ingestion.entity.TeamRecord;
import com.sportstock.ingestion.mapper.JsonPayloadCodec;
import com.sportstock.ingestion.repo.TeamRecordRepository;
import com.sportstock.ingestion.repo.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamIngestionServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private EspnApiClient espnApiClient;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private TeamRecordRepository teamRecordRepository;
    @Mock
    private JsonPayloadCodec jsonPayloadCodec;
    @Mock
    private TransactionTemplate transactionTemplate;

    private TeamIngestionService service;
    private JsonNode teamDetailNode;

    @BeforeEach
    void setUp() throws Exception {
        service = new TeamIngestionService(
                espnApiClient,
                teamRepository,
                teamRecordRepository,
                jsonPayloadCodec,
                transactionTemplate
        );
        teamDetailNode = OBJECT_MAPPER.readTree("""
                {
                  "team": {
                    "id": "1",
                    "uid": "s:20~l:28~t:1",
                    "slug": "atlanta-falcons",
                    "abbreviation": "ATL",
                    "displayName": "Atlanta Falcons",
                    "shortDisplayName": "Falcons",
                    "name": "Falcons",
                    "nickname": "Falcons",
                    "location": "Atlanta",
                    "color": "a71930",
                    "alternateColor": "000000",
                    "isActive": true,
                    "isAllStar": false,
                    "logos": [{"href": "https://example.com/logo.png"}],
                    "record": {"items": []}
                  }
                }
                """);
    }

    @Test
    void ingestAllTeamDetailsContinuesAfterPerTeamFailure() {
        runTransactionTemplateCallbacks();

        Team t1 = team("1");
        Team t2 = team("2");
        Team t3 = team("3");
        when(teamRepository.findAllByOrderByDisplayNameAsc()).thenReturn(List.of(t1, t2, t3));
        when(espnApiClient.fetchTeamDetail("1")).thenReturn("team-1");
        when(espnApiClient.fetchTeamDetail("3")).thenReturn("team-3");
        when(espnApiClient.fetchTeamDetail("2")).thenThrow(new RuntimeException("boom"));
        when(jsonPayloadCodec.parseJson(anyString())).thenReturn(teamDetailNode);
        when(teamRepository.findByEspnId(anyString())).thenReturn(Optional.empty());
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> service.ingestAllTeamDetails(2025));

        verify(espnApiClient).fetchTeamDetail("1");
        verify(espnApiClient).fetchTeamDetail("2");
        verify(espnApiClient).fetchTeamDetail("3");
        verify(transactionTemplate, times(3)).executeWithoutResult(any());
    }

    @Test
    void ingestTeamDetailUsesFullLengthRecordTypeForLookupAndSave() throws Exception {
        String longRecordType = "overall-season-long-record-type";
        JsonNode teamDetailWithRecord = OBJECT_MAPPER.readTree("""
                {
                  "team": {
                    "id": "1",
                    "uid": "s:20~l:28~t:1",
                    "slug": "atlanta-falcons",
                    "abbreviation": "ATL",
                    "displayName": "Atlanta Falcons",
                    "shortDisplayName": "Falcons",
                    "name": "Falcons",
                    "nickname": "Falcons",
                    "location": "Atlanta",
                    "color": "a71930",
                    "alternateColor": "000000",
                    "isActive": true,
                    "isAllStar": false,
                    "logos": [{"href": "https://example.com/logo.png"}],
                    "record": {
                      "items": [
                        {
                          "type": "%s",
                          "summary": "10-7",
                          "stats": [
                            {"name": "wins", "value": 10},
                            {"name": "losses", "value": 7}
                          ]
                        }
                      ]
                    }
                  }
                }
                """.formatted(longRecordType));

        Team existingTeam = team("1");
        existingTeam.setId(1L);

        when(espnApiClient.fetchTeamDetail("1")).thenReturn("team-1");
        when(jsonPayloadCodec.parseJson("team-1")).thenReturn(teamDetailWithRecord);
        when(teamRepository.findByEspnId("1")).thenReturn(Optional.of(existingTeam));
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(teamRecordRepository.findByTeamIdAndSeasonYearAndRecordType(1L, 2025, longRecordType))
                .thenReturn(Optional.empty());
        when(teamRecordRepository.save(any(TeamRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> service.ingestTeamDetail("1", 2025));

        ArgumentCaptor<TeamRecord> recordCaptor = ArgumentCaptor.forClass(TeamRecord.class);
        verify(teamRecordRepository).findByTeamIdAndSeasonYearAndRecordType(1L, 2025, longRecordType);
        verify(teamRecordRepository).save(recordCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(longRecordType, recordCaptor.getValue().getRecordType());
    }

    @Test
    void ingestTeamDetailReusesExistingRecordForLongRecordType() throws Exception {
        String longRecordType = "overall-season-long-record-type";
        JsonNode teamDetailWithRecord = OBJECT_MAPPER.readTree("""
                {
                  "team": {
                    "id": "1",
                    "uid": "s:20~l:28~t:1",
                    "slug": "atlanta-falcons",
                    "abbreviation": "ATL",
                    "displayName": "Atlanta Falcons",
                    "shortDisplayName": "Falcons",
                    "name": "Falcons",
                    "nickname": "Falcons",
                    "location": "Atlanta",
                    "color": "a71930",
                    "alternateColor": "000000",
                    "isActive": true,
                    "isAllStar": false,
                    "logos": [{"href": "https://example.com/logo.png"}],
                    "record": {
                      "items": [
                        {
                          "type": "%s",
                          "summary": "10-7",
                          "stats": [
                            {"name": "wins", "value": 10},
                            {"name": "losses", "value": 7}
                          ]
                        }
                      ]
                    }
                  }
                }
                """.formatted(longRecordType));

        Team existingTeam = team("1");
        existingTeam.setId(1L);
        TeamRecord existingRecord = new TeamRecord();
        existingRecord.setRecordType(longRecordType);

        when(espnApiClient.fetchTeamDetail("1")).thenReturn("team-1");
        when(jsonPayloadCodec.parseJson("team-1")).thenReturn(teamDetailWithRecord);
        when(teamRepository.findByEspnId("1")).thenReturn(Optional.of(existingTeam));
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(teamRecordRepository.findByTeamIdAndSeasonYearAndRecordType(1L, 2025, longRecordType))
                .thenReturn(Optional.of(existingRecord));
        when(teamRecordRepository.save(any(TeamRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> service.ingestTeamDetail("1", 2025));

        ArgumentCaptor<TeamRecord> recordCaptor = ArgumentCaptor.forClass(TeamRecord.class);
        verify(teamRecordRepository).findByTeamIdAndSeasonYearAndRecordType(1L, 2025, longRecordType);
        verify(teamRecordRepository).save(recordCaptor.capture());
        assertSame(existingRecord, recordCaptor.getValue());
        org.junit.jupiter.api.Assertions.assertEquals(longRecordType, recordCaptor.getValue().getRecordType());
    }

    @Test
    void getRecordUsesFullLengthRecordType() {
        String longRecordType = "overall-season-long-record-type";
        Team existingTeam = team("1");
        existingTeam.setId(1L);
        TeamRecord existingRecord = new TeamRecord();
        existingRecord.setRecordType(longRecordType);

        when(teamRepository.findByEspnId("1")).thenReturn(Optional.of(existingTeam));
        when(teamRecordRepository.findByTeamIdAndSeasonYearAndRecordType(1L, 2025, longRecordType))
                .thenReturn(Optional.of(existingRecord));

        TeamRecord result = service.getRecord("1", 2025, longRecordType);

        assertSame(existingRecord, result);
        verify(teamRecordRepository).findByTeamIdAndSeasonYearAndRecordType(1L, 2025, longRecordType);
        verify(teamRecordRepository, never()).save(any(TeamRecord.class));
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
