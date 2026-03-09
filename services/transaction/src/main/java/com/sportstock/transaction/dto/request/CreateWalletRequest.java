package com.sportstock.transaction.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateWalletRequest(
        @NotNull Long leagueId
) {
}
