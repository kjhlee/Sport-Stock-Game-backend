package com.sportstock.transaction.exception;

public class WalletAlreadyExistsException extends TransactionException {

  public WalletAlreadyExistsException(String message) {
    super(message);
  }

  public WalletAlreadyExistsException(Long userId, Long leagueId) {
    super(String.format("Wallet already exists for userId=%d, leagueId=%d", userId, leagueId));
  }
}
