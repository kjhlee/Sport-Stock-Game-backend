package com.sportstock.common.dto.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record StockTransactionRequest(
    @NotNull Long leagueId,
    @NotNull UUID stockId,
    BigDecimal quantity,
    BigDecimal dollarAmount,
    @NotBlank String idempotencyKey) {}
