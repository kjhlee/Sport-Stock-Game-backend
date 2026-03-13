package com.sportstock.common.dto.league;

import java.time.OffsetDateTime;

public record LeagueInviteResponse(
    Long id,
    String code,
    OffsetDateTime expiresAt,
    Integer maxUses,
    Integer usesCount,
    OffsetDateTime createdAt) {}
