package com.sportstock.transaction.mapper;

import com.sportstock.common.dto.transaction.TransactionResponse;
import com.sportstock.common.dto.transaction.WalletResponse;
import com.sportstock.transaction.entity.Transaction;
import com.sportstock.transaction.entity.Wallet;

public final class DtoMapper {

  private DtoMapper() {}

  public static WalletResponse toWalletResponse(Wallet entity) {
    return new WalletResponse(
        entity.getId(),
        entity.getUserId(),
        entity.getLeagueId(),
        entity.getBalance(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public static TransactionResponse toTransactionResponse(Transaction entity) {
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
        entity.getCreatedAt());
  }
}
