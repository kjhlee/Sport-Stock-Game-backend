package com.sportstock.league.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinLeagueRequest(
        @NotBlank @Size(max = 64) String inviteCode
) {
}
