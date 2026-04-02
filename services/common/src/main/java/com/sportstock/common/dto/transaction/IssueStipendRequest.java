package com.sportstock.common.dto.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record IssueStipendRequest(
    @NotNull Long leagueId,
    @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
    List<Long> userIds) {}
