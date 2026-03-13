package com.example.user_authentication.security.exceptions;

public class InvalidTokenException extends RuntimeException {
  public InvalidTokenException(String message) {
    super("Token is invalid: " + message);
  }
}
