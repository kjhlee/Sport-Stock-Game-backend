package com.sportstock.ingestion.dto.response;

import com.sportstock.ingestion.entity.Event;

import java.math.BigDecimal;
import java.time.Instant;

public record EventResponse(
        String espnId,
        String name,
        String shortName,
        Instant date,
        Integer seasonYear,
        Integer seasonType,
        String seasonSlug,
        Integer weekNumber,
        Integer attendance,
        Boolean neutralSite,
        Boolean conferenceCompetition,
        Boolean playByPlayAvailable,
        String statusState,
        Boolean statusCompleted,
        String statusDescription,
        Integer statusPeriod,
        BigDecimal statusClock,
        String broadcast,
        String noteHeadline
) {
    public static EventResponse from(Event entity) {
        return new EventResponse(
                entity.getEspnId(),
                entity.getName(),
                entity.getShortName(),
                entity.getDate(),
                entity.getSeasonYear(),
                entity.getSeasonType(),
                entity.getSeasonSlug(),
                entity.getWeekNumber(),
                entity.getAttendance(),
                entity.getNeutralSite(),
                entity.getConferenceCompetition(),
                entity.getPlayByPlayAvailable(),
                entity.getStatusState(),
                entity.getStatusCompleted(),
                entity.getStatusDescription(),
                entity.getStatusPeriod(),
                entity.getStatusClock(),
                entity.getBroadcast(),
                entity.getNoteHeadline()
        );
    }
}
