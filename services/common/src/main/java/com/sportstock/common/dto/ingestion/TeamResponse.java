package com.sportstock.common.dto.ingestion;

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
    String standingSummary) {}
