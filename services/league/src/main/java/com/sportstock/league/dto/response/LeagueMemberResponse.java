package com.sportstock.league.dto.response;

import com.sportstock.league.entity.LeagueMember;

import java.time.OffsetDateTime;

public record LeagueMemberResponse(
        Long id,
        Long userId,
        String role,
        OffsetDateTime joinedAt
) {
    public static LeagueMemberResponse from(LeagueMember entity) {
        return new LeagueMemberResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getRole(),
                entity.getJoinedAt()
        );
    }
}
