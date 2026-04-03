package com.sportstock.common.dto.transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
        Long id,
        Long walletId,
        String type,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        Long leagueId,
        Long userId,
        String referenceId,
        String description,
        String idempotencyKey,
        BigDecimal pricePerShare,
        Long buyTransactionId,
        OffsetDateTime createdAt) {}
