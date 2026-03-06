package com.sportstock.league.dto.response;

import com.sportstock.league.entity.League;

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
    public static LeagueResponse from(League entity, int memberCount) {
        return new LeagueResponse(
                entity.getId(),
                entity.getOwnerUserId(),
                entity.getName(),
                entity.getStatus(),
                entity.getMaxMembers(),
                entity.getSeasonStartAt(),
                entity.getSeasonEndAt(),
                entity.getInitialStipendAmount(),
                entity.getWeeklyStipendAmount(),
                entity.getWeeklyPayoutDowUtc(),
                entity.getStartedAt(),
                entity.getCreatedAt(),
                memberCount
        );
    }
}
