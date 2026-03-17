package com.sportstock.common.dto.transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record WalletResponse(
    Long id,
    Long userId,
    Long leagueId,
    BigDecimal balance,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
