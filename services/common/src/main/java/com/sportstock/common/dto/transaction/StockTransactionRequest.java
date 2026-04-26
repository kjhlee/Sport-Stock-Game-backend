package com.sportstock.common.dto.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record StockTransactionRequest(
    @NotNull @Positive Long leagueId,
    @NotNull UUID stockId,
    @DecimalMin(value = "0.0001") @Digits(integer = 19, fraction = 4) BigDecimal quantity,
    @DecimalMin(value = "0.0001") @Digits(integer = 19, fraction = 4) BigDecimal dollarAmount,
    @Positive Long buyTransactionId,
    @NotBlank String idempotencyKey) {}
