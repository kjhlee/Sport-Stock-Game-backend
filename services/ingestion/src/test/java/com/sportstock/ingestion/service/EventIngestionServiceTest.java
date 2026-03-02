package com.sportstock.ingestion.service;

import com.sportstock.ingestion.client.EspnApiClient;
import com.sportstock.ingestion.entity.Event;
import com.sportstock.ingestion.mapper.JsonPayloadCodec;
import com.sportstock.ingestion.repo.EventCompetitorLinescoreRepository;
import com.sportstock.ingestion.repo.EventCompetitorRepository;
import com.sportstock.ingestion.repo.EventRepository;
import com.sportstock.ingestion.repo.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventIngestionServiceTest {

    @Mock
    private EspnApiClient espnApiClient;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventCompetitorRepository eventCompetitorRepository;
    @Mock
    private EventCompetitorLinescoreRepository eventCompetitorLinescoreRepository;
    @Mock
    private TeamRepository teamRepository;
    @Mock
    private SeasonIngestionService seasonIngestionService;
    @Mock
    private JsonPayloadCodec jsonPayloadCodec;

    private EventIngestionService service;

    @BeforeEach
    void setUp() {
        service = new EventIngestionService(
                espnApiClient,
                eventRepository,
                eventCompetitorRepository,
                eventCompetitorLinescoreRepository,
                teamRepository,
                seasonIngestionService,
                jsonPayloadCodec
        );
    }

    @Test
    void listEventsWithoutWeekReturnsSeasonYearQuery() {
        List<Event> expected = List.of(new Event());
        when(eventRepository.findBySeasonYearOrderByDateAsc(2025)).thenReturn(expected);

        List<Event> result = service.listEvents(2025, null, null);

        assertSame(expected, result);
        verify(eventRepository).findBySeasonYearOrderByDateAsc(2025);
    }

    @Test
    void listEventsWithWeekUsesSeasonTypeQuery() {
        List<Event> expected = List.of(new Event());
        when(eventRepository.findBySeasonYearAndSeasonTypeAndWeekNumberOrderByDateAsc(2025, 2, 1))
                .thenReturn(expected);

        List<Event> result = service.listEvents(2025, 2, 1);

        assertSame(expected, result);
        verify(eventRepository).findBySeasonYearAndSeasonTypeAndWeekNumberOrderByDateAsc(2025, 2, 1);
    }

    @Test
    void listEventsWithWeekWithoutSeasonTypeThrows() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.listEvents(2025, null, 1)
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                "seasonType is required when weekNumber is provided",
                ex.getMessage()
        );
        verifyNoInteractions(eventRepository);
    }
}
