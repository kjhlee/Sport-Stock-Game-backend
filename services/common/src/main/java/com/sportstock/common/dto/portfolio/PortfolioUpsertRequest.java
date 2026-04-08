package com.sportstock.common.dto.portfolio;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PortfolioUpsertRequest(@NotNull @Positive Long userId, @NotNull @Positive Long leagueId) {}
