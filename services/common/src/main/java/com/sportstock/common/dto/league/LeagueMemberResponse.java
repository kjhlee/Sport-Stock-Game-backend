package com.sportstock.common.dto.league;

import java.time.OffsetDateTime;

public record LeagueMemberResponse(Long id, Long userId, String role, OffsetDateTime joinedAt) {}
