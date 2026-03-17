package com.sportstock.common.dto.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record IssueStipendRequest(
    @NotNull Long leagueId,
    @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount) {}
