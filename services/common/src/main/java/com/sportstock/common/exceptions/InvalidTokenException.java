package com.sportstock.common.exceptions;

public class InvalidTokenException extends RuntimeException {
  public InvalidTokenException(String message, String token) {
    super("Token is invalid: " + message);
  }
}
