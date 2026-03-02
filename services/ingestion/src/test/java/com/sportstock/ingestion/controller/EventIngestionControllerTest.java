package com.sportstock.ingestion.controller;

import com.sportstock.ingestion.service.EventIngestionService;
import com.sportstock.ingestion.service.EventSummaryIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventIngestionControllerTest {

    @Mock
    private EventIngestionService eventIngestionService;
    @Mock
    private EventSummaryIngestionService eventSummaryIngestionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        EventIngestionController controller =
                new EventIngestionController(eventIngestionService, eventSummaryIngestionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listEventsReturns400WhenWeekNumberProvidedWithoutSeasonType() throws Exception {
        mockMvc.perform(get("/api/v1/ingestion/events")
                        .param("seasonYear", "2025")
                        .param("weekNumber", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("seasonType is required when weekNumber is provided"));
    }

    @Test
    void listEventsReturns200WhenSeasonTypeAndWeekProvided() throws Exception {
        when(eventIngestionService.listEvents(2025, 2, 1)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/ingestion/events")
                        .param("seasonYear", "2025")
                        .param("seasonType", "2")
                        .param("weekNumber", "1"))
                .andExpect(status().isOk());

        verify(eventIngestionService).listEvents(2025, 2, 1);
    }

    @Test
    void listEventsReturns200WhenOnlySeasonYearProvided() throws Exception {
        when(eventIngestionService.listEvents(2025, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/ingestion/events")
                        .param("seasonYear", "2025"))
                .andExpect(status().isOk());

        verify(eventIngestionService).listEvents(2025, null, null);
    }
}
