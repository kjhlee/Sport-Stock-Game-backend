package com.sportstock.common.exceptions;

public class TokenExpiredException extends RuntimeException {
  public TokenExpiredException(String message, String token) {
    super("Token: " + token + " is expired: " + message);
  }
}
