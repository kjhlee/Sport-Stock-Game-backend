package com.sportstock.common.dto.transaction;

import jakarta.validation.constraints.NotNull;

public record CreateWalletRequest(@NotNull Long leagueId) {}
