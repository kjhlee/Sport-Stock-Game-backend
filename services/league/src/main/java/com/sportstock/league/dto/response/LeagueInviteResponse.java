package com.sportstock.league.dto.response;

import com.sportstock.league.entity.LeagueInvite;

import java.time.OffsetDateTime;

public record LeagueInviteResponse(
        Long id,
        String code,
        OffsetDateTime expiresAt,
        Integer maxUses,
        Integer usesCount,
        OffsetDateTime createdAt
) {
    public static LeagueInviteResponse from(LeagueInvite entity) {
        return new LeagueInviteResponse(
                entity.getId(),
                entity.getCode(),
                entity.getExpiresAt(),
                entity.getMaxUses(),
                entity.getUsesCount(),
                entity.getCreatedAt()
        );
    }
}
