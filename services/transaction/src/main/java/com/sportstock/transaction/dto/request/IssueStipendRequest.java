package com.sportstock.transaction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record IssueStipendRequest(
        @NotNull Long leagueId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        @NotEmpty List<Long> userIds
) {
}
