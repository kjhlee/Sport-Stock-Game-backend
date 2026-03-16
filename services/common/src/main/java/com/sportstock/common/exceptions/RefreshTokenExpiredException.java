package com.sportstock.common.exceptions;

public class RefreshTokenExpiredException extends RuntimeException {
  public RefreshTokenExpiredException(String message, String token) {
    super("Refresh token is expired: " + message);
  }
}
