package com.sportstock.common.exceptions;

public class RefreshTokenExpiredException extends RuntimeException {
  public RefreshTokenExpiredException(String message, String token) {
    super("Token: " + token + " is an expired refresh token: " + message);
  }
}
