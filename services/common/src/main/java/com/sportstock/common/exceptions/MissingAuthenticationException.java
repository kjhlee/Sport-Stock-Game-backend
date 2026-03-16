package com.sportstock.common.exceptions;

public class MissingAuthenticationException extends IllegalStateException {

  public MissingAuthenticationException(String message) {
    super(message);
  }

  public MissingAuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}
