package com.sportstock.common.dto.transaction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateWalletRequest(@NotNull @Positive Long leagueId) {}
