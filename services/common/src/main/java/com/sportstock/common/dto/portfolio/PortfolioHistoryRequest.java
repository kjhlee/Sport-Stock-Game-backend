package com.sportstock.common.dto.portfolio;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PortfolioHistoryRequest(
    @NotNull @Positive Long userId,
    @NotNull @Positive Long leagueId,
    @NotNull @Positive Integer weekNumber,
    @NotBlank String seasonType,
    @NotNull @DecimalMin("0.0000") @Digits(integer = 19, fraction = 4) BigDecimal value) {}
