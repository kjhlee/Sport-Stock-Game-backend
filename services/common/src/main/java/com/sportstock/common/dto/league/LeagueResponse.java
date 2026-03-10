package com.sportstock.common.dto.league;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LeagueResponse(
        Long id,
        Long ownerUserId,
        String name,
        String status,
        Integer maxMembers,
        OffsetDateTime seasonStartAt,
        OffsetDateTime seasonEndAt,
        BigDecimal initialStipendAmount,
        BigDecimal weeklyStipendAmount,
        Short weeklyPayoutDowUtc,
        OffsetDateTime startedAt,
        OffsetDateTime createdAt,
        int memberCount
) {
}
