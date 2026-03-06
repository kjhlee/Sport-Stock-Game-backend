package com.sportstock.league.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;

public record CreateInviteRequest(
        @Future OffsetDateTime expiresAt,
        @Positive Integer maxUses
) {
}
