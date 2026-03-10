package com.sportstock.common.dto.league;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinLeagueRequest(
        @NotBlank @Size(max = 64) String inviteCode
) {
}
