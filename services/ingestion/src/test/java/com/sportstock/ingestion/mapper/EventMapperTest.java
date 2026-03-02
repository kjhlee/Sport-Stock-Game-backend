package com.sportstock.ingestion.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportstock.ingestion.entity.Event;
import com.sportstock.ingestion.exception.IngestionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventMapperTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void applyFieldsReadsSeasonTypeFromNestedTypeObject() throws Exception {
        JsonNode eventNode = OBJECT_MAPPER.readTree("""
                {
                  "id": "401",
                  "name": "Example Event",
                  "shortName": "EX",
                  "date": "2025-09-01T17:00:00Z",
                  "season": {
                    "year": 2025,
                    "type": {
                      "id": "2"
                    },
                    "slug": "regular-season"
                  }
                }
                """);

        Event event = new Event();

        EventMapper.applyFields(eventNode, null, event, 3);

        assertEquals(2025, event.getSeasonYear());
        assertEquals(2, event.getSeasonType());
    }

    @Test
    void applyFieldsFallsBackToRequestedSeasonTypeWhenMissingFromPayload() throws Exception {
        JsonNode eventNode = OBJECT_MAPPER.readTree("""
                {
                  "id": "402",
                  "name": "Example Event",
                  "shortName": "EX",
                  "date": "2025-09-01T17:00:00Z",
                  "season": {
                    "year": 2025,
                    "slug": "regular-season"
                  }
                }
                """);

        Event event = new Event();

        EventMapper.applyFields(eventNode, null, event, 2);

        assertEquals(2, event.getSeasonType());
    }

    @Test
    void applyFieldsThrowsWhenDateIsMissing() throws Exception {
        JsonNode eventNode = OBJECT_MAPPER.readTree("""
                {
                  "id": "403",
                  "name": "Example Event",
                  "shortName": "EX",
                  "season": {
                    "year": 2025,
                    "type": {
                      "id": "2"
                    }
                  }
                }
                """);

        Event event = new Event();

        IngestionException ex = assertThrows(
                IngestionException.class,
                () -> EventMapper.applyFields(eventNode, null, event, 2)
        );

        assertEquals("Event 403 is missing a parseable date", ex.getMessage());
    }
}
