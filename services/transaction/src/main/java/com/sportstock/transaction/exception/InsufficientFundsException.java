package com.sportstock.transaction.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends TransactionException {

  public InsufficientFundsException(String message) {
    super(message);
  }

  public InsufficientFundsException(BigDecimal balance, BigDecimal required) {
    super(String.format("Insufficient funds: balance=%s, required=%s", balance, required));
  }
}
