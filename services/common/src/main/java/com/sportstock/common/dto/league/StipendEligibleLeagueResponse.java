package com.sportstock.common.dto.league;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StipendEligibleLeagueResponse(
    Long leagueId, BigDecimal weeklyStipendAmount, OffsetDateTime initialStipendIssuedAt) {}
