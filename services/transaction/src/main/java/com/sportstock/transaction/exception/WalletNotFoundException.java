package com.sportstock.transaction.exception;

public class WalletNotFoundException extends TransactionException {

  public WalletNotFoundException(String message) {
    super(message);
  }

  public WalletNotFoundException(Long userId, Long leagueId) {
    super(String.format("Wallet not found for userId=%d, leagueId=%d", userId, leagueId));
  }
}
