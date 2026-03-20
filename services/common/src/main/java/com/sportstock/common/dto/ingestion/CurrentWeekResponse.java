package com.sportstock.common.dto.ingestion;

import java.time.Instant;

public record CurrentWeekResponse(
    Integer seasonYear,
    String seasonType,
    String seasonTypeName,
    Integer week,
    String weekLabel,
    Instant startDate,
    Instant endDate) {}
