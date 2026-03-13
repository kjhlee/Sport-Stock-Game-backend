package com.sportstock.common.dto.ingestion;

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
    String noteHeadline) {}
