package com.sportstock.ingestion.dto.response;

import com.sportstock.ingestion.entity.Team;

public record TeamResponse(
        String espnId,
        String abbreviation,
        String displayName,
        String shortDisplayName,
        String name,
        String nickname,
        String location,
        String color,
        String alternateColor,
        Boolean isActive,
        Boolean isAllStar,
        String logoUrl,
        String divisionId,
        String conferenceId,
        String standingSummary
) {
    public static TeamResponse from(Team entity) {
        return new TeamResponse(
                entity.getEspnId(),
                entity.getAbbreviation(),
                entity.getDisplayName(),
                entity.getShortDisplayName(),
                entity.getName(),
                entity.getNickname(),
                entity.getLocation(),
                entity.getColor(),
                entity.getAlternateColor(),
                entity.getIsActive(),
                entity.getIsAllStar(),
                entity.getLogoUrl(),
                entity.getDivisionId(),
                entity.getConferenceId(),
                entity.getStandingSummary()
        );
    }
}
