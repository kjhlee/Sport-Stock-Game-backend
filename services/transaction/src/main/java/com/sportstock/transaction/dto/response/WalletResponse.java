package com.sportstock.transaction.dto.response;

import com.sportstock.transaction.entity.Wallet;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record WalletResponse(
        Long id,
        Long userId,
        Long leagueId,
        BigDecimal balance,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static WalletResponse from(Wallet entity) {
        return new WalletResponse(
                entity.getId(),
                entity.getUserId(),
                entity.getLeagueId(),
                entity.getBalance(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
