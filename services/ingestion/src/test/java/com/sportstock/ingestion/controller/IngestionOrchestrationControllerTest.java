package com.sportstock.ingestion.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sportstock.ingestion.service.IngestionOrchestrationService;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class IngestionOrchestrationControllerTest {

  @Mock private IngestionOrchestrationService ingestionOrchestrationService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    IngestionOrchestrationController controller =
        new IngestionOrchestrationController(ingestionOrchestrationService);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void syncFoundationReturns202WhenAccepted() throws Exception {
    when(ingestionOrchestrationService.tryStartFoundationSync(2025, 2, 3)).thenReturn(true);

    mockMvc
        .perform(
            post("/api/v1/ingestion/sync/foundation")
                .param("seasonYear", "2025")
                .param("seasonType", "2")
                .param("week", "3"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.jobName").value("foundationSync"))
        .andExpect(jsonPath("$.status").value("ACCEPTED"));

    verify(ingestionOrchestrationService).runFoundationSync(2025, 2, 3);
  }

  @Test
  void syncFoundationReturns409WhenRejectedByMutex() throws Exception {
    when(ingestionOrchestrationService.tryStartFoundationSync(2025, 2, 3)).thenReturn(false);

    mockMvc
        .perform(
            post("/api/v1/ingestion/sync/foundation")
                .param("seasonYear", "2025")
                .param("seasonType", "2")
                .param("week", "3"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.jobName").value("foundationSync"))
        .andExpect(jsonPath("$.status").value("REJECTED_BUSY"));

    verify(ingestionOrchestrationService, never()).runFoundationSync(anyInt(), anyInt(), anyInt());
  }

  @Test
  void syncFoundationReturns503WhenExecutorRejects() throws Exception {
    when(ingestionOrchestrationService.tryStartFoundationSync(2025, 2, 3)).thenReturn(true);
    doThrow(new RejectedExecutionException("queue full"))
        .when(ingestionOrchestrationService)
        .runFoundationSync(2025, 2, 3);

    mockMvc
        .perform(
            post("/api/v1/ingestion/sync/foundation")
                .param("seasonYear", "2025")
                .param("seasonType", "2")
                .param("week", "3"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.jobName").value("foundationSync"))
        .andExpect(jsonPath("$.status").value("SERVICE_UNAVAILABLE"));

    verify(ingestionOrchestrationService).cancelFoundationSyncStart(2025, 2, 3);
  }

  @Test
  void syncFullReturns409WhenRejectedByMutex() throws Exception {
    when(ingestionOrchestrationService.tryStartFullSync()).thenReturn(false);

    mockMvc
        .perform(
            post("/api/v1/ingestion/sync/full")
                .param("seasonYear", "2025")
                .param("seasonType", "2")
                .param("week", "3")
                .param("rosterLimit", "100")
                .param("athletePageSize", "100")
                .param("athletePageCount", "1"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.jobName").value("fullSync"))
        .andExpect(jsonPath("$.status").value("REJECTED_BUSY"));
  }

  @Test
  void syncFullReturns503WhenExecutorRejects() throws Exception {
    when(ingestionOrchestrationService.tryStartFullSync()).thenReturn(true);
    doThrow(new RejectedExecutionException("queue full"))
        .when(ingestionOrchestrationService)
        .runFullSync(2025, 2, 3, 100, 100, 1, null);

    mockMvc
        .perform(
            post("/api/v1/ingestion/sync/full")
                .param("seasonYear", "2025")
                .param("seasonType", "2")
                .param("week", "3")
                .param("rosterLimit", "100")
                .param("athletePageSize", "100")
                .param("athletePageCount", "1"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.jobName").value("fullSync"))
        .andExpect(jsonPath("$.status").value("SERVICE_UNAVAILABLE"));

    verify(ingestionOrchestrationService).cancelFullSyncStart();
  }

  @Test
  void syncWeeklyReturns202WhenAccepted() throws Exception {
    when(ingestionOrchestrationService.tryStartWeeklySync(2025, 2, 4)).thenReturn(true);

    mockMvc
        .perform(
            post("/api/v1/ingestion/sync/weekly")
                .param("seasonYear", "2025")
                .param("seasonType", "2")
                .param("week", "4"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.jobName").value("weeklySync"))
        .andExpect(jsonPath("$.status").value("ACCEPTED"));

    verify(ingestionOrchestrationService).runWeeklySync(eq(2025), eq(2), eq(4));
  }

  @Test
  void syncWeeklyReturns503WhenExecutorRejects() throws Exception {
    when(ingestionOrchestrationService.tryStartWeeklySync(2025, 2, 4)).thenReturn(true);
    doThrow(new RejectedExecutionException("queue full"))
        .when(ingestionOrchestrationService)
        .runWeeklySync(2025, 2, 4);

    mockMvc
        .perform(
            post("/api/v1/ingestion/sync/weekly")
                .param("seasonYear", "2025")
                .param("seasonType", "2")
                .param("week", "4"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.jobName").value("weeklySync"))
        .andExpect(jsonPath("$.status").value("SERVICE_UNAVAILABLE"));

    verify(ingestionOrchestrationService).cancelWeeklySyncStart(2025, 2, 4);
  }
}
