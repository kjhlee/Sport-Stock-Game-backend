package com.sportstock.common.dto.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StockTransactionRequest(
        @NotNull Long leagueId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount,
        @NotBlank String referenceId,
        String description
) {
}
