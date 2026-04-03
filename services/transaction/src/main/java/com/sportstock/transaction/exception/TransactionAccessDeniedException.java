package com.sportstock.transaction.exception;

public class TransactionAccessDeniedException extends TransactionException {

  public TransactionAccessDeniedException(String message) {
    super(message);
  }
}
