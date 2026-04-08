package com.sportstock.common.dto.portfolio;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ProcessSellRequest(
    @NotNull UUID stockId,
    @NotNull @DecimalMin("0.0001") @Digits(integer = 19, fraction = 4) BigDecimal quantity) {}
