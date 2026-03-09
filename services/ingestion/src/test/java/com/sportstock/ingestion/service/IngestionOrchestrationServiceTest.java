package com.sportstock.ingestion.service;

import com.sportstock.common.dto.ingestion.EventResponse;
import com.sportstock.common.dto.ingestion.TeamResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestionOrchestrationServiceTest {

    @Mock
    private TeamIngestionService teamIngestionService;
    @Mock
    private RosterIngestionService rosterIngestionService;
    @Mock
    private AthleteIngestionService athleteIngestionService;
    @Mock
    private EventIngestionService eventIngestionService;
    @Mock
    private EventSummaryIngestionService eventSummaryIngestionService;
    @Mock
    private TransactionTemplate transactionTemplate;

    private IngestionOrchestrationService service;

    @BeforeEach
    void setUp() {
        service = new IngestionOrchestrationService(
                teamIngestionService,
                rosterIngestionService,
                athleteIngestionService,
                eventIngestionService,
                eventSummaryIngestionService,
                transactionTemplate
        );
    }

    @Test
    void tryStartFullSyncRejectedWhenWindowJobRunning() {
        assertTrue(service.tryStartFoundationSync(2025, 2, 1));
        assertFalse(service.tryStartFullSync());
    }

    @Test
    void tryStartWindowJobRejectedWhenFullSyncRunning() {
        assertTrue(service.tryStartFullSync());
        assertFalse(service.tryStartWeeklySync(2025, 2, 1));
    }

    @Test
    void cancelMethodsReleaseReservations() {
        assertTrue(service.tryStartFoundationSync(2025, 2, 1));
        service.cancelFoundationSyncStart(2025, 2, 1);
        assertTrue(service.tryStartFoundationSync(2025, 2, 1));
        service.cancelFoundationSyncStart(2025, 2, 1);

        assertTrue(service.tryStartFullSync());
        service.cancelFullSyncStart();
        assertTrue(service.tryStartWeeklySync(2025, 2, 3));
    }

    @Test
    void cancellingWrongWindowDoesNotFreeActiveWindowJob() {
        assertTrue(service.tryStartFoundationSync(2025, 2, 1));
        service.cancelFoundationSyncStart(2025, 2, 99);

        assertFalse(service.tryStartFullSync());
    }

    @Test
    void sameWindowCannotStartTwiceButDifferentWindowCan() {
        assertTrue(service.tryStartFoundationSync(2025, 2, 1));
        assertFalse(service.tryStartWeeklySync(2025, 2, 1));
        assertTrue(service.tryStartWeeklySync(2025, 2, 2));
    }

    @Test
    void runFoundationSyncReleasesWindowLockOnFailure() {
        assertTrue(service.tryStartFoundationSync(2025, 2, 1));
        doThrow(new RuntimeException("boom"))
                .when(eventIngestionService).ingestScoreboard(2025, 2, 1);

        assertThrows(RuntimeException.class, () -> service.runFoundationSync(2025, 2, 1));
        assertTrue(service.tryStartFoundationSync(2025, 2, 1));
    }

    @Test
    void runFullSyncReleasesFullSyncLockOnFailure() {
        assertTrue(service.tryStartFullSync());
        doThrow(new RuntimeException("boom"))
                .when(eventIngestionService).ingestScoreboard(2025, 2, 1);

        assertThrows(RuntimeException.class, () ->
                service.runFullSync(2025, 2, 1, 100, 100, 1, null));
        assertTrue(service.tryStartFullSync());
    }

    @Test
    void runWeeklySyncContinuesWhenOneEventSummaryFails() {
        runTransactionTemplateCallbacks();
        assertTrue(service.tryStartWeeklySync(2025, 2, 1));

        EventResponse eventOne = eventResponse("100");
        EventResponse eventTwo = eventResponse("200");
        when(eventIngestionService.listEvents(2025, 2, 1)).thenReturn(List.of(eventOne, eventTwo));
        doThrow(new RuntimeException("event summary failed"))
                .when(eventSummaryIngestionService).ingestEventSummary("100");

        assertDoesNotThrow(() -> service.runWeeklySync(2025, 2, 1));

        verify(eventSummaryIngestionService).ingestEventSummary("100");
        verify(eventSummaryIngestionService).ingestEventSummary("200");
        assertTrue(service.tryStartWeeklySync(2025, 2, 1));
    }

    @Test
    void runFullSyncContinuesAfterTeamAndEventFailures() {
        runTransactionTemplateCallbacks();
        assertTrue(service.tryStartFullSync());

        TeamResponse teamOne = teamResponse("1");
        TeamResponse teamTwo = teamResponse("2");
        when(teamIngestionService.listTeams()).thenReturn(List.of(teamOne, teamTwo));

        EventResponse eventOne = eventResponse("100");
        EventResponse eventTwo = eventResponse("200");
        when(eventIngestionService.listEvents(2025, 2, 1)).thenReturn(List.of(eventOne, eventTwo));

        doThrow(new RuntimeException("team detail failed"))
                .when(teamIngestionService).ingestTeamDetail("1", 2025);
        doThrow(new RuntimeException("event summary failed"))
                .when(eventSummaryIngestionService).ingestEventSummary("100");

        assertDoesNotThrow(() ->
                service.runFullSync(2025, 2, 1, 100, 100, 1, null));

        verify(teamIngestionService).ingestTeamDetail("1", 2025);
        verify(teamIngestionService).ingestTeamDetail("2", 2025);
        verify(eventSummaryIngestionService).ingestEventSummary("100");
        verify(eventSummaryIngestionService).ingestEventSummary("200");
        verify(rosterIngestionService).ingestAllRosters(2025, 100, null);
        verify(athleteIngestionService).ingestAthletes(100, 1);
        verify(eventIngestionService).listEvents(2025, 2, 1);

        assertTrue(service.tryStartFullSync());
        assertFalse(service.tryStartFoundationSync(2025, 2, 1));
    }

    private void runTransactionTemplateCallbacks() {
        doAnswer(invocation -> {
            Consumer<TransactionStatus> action = invocation.getArgument(0);
            action.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    private TeamResponse teamResponse(String espnId) {
        return new TeamResponse(espnId, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private EventResponse eventResponse(String espnId) {
        return new EventResponse(espnId, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
