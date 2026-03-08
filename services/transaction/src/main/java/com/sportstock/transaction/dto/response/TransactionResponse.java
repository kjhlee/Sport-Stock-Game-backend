package com.sportstock.transaction.dto.response;

import com.sportstock.transaction.entity.Transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
        OffsetDateTime createdAt
) {
    public static TransactionResponse from(Transaction entity) {
        return new TransactionResponse(
                entity.getId(),
                entity.getWallet().getId(),
                entity.getType().name(),
                entity.getAmount(),
                entity.getBalanceBefore(),
                entity.getBalanceAfter(),
                entity.getLeagueId(),
                entity.getUserId(),
                entity.getReferenceId(),
                entity.getDescription(),
                entity.getIdempotencyKey(),
                entity.getCreatedAt()
        );
    }
}
