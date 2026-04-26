package com.sportstock.common.dto.ingestion;

import java.math.BigDecimal;
import java.time.Instant;

public record FantasySnapshotResponse(
    Long id,
    String eventEspnId,
    String subjectType,
    String espnId,
    String fullName,
    String projectedStats,
    BigDecimal projectedFantasyPoints,
    BigDecimal actualFantasyPoints,
    boolean completed,
    Instant updatedAt) {}
