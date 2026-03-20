package com.sportstock.transaction.exception;

public class StockNotActiveException extends TransactionException {

  public StockNotActiveException(String stockId, String status) {
    super(String.format("Stock %s is not active (status: %s)", stockId, status));
  }
}
