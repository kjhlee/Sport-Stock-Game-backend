package com.sportstock.user_authentication.exception;

import java.util.Map;
import lombok.Getter;

@Getter
public class RegistrationConflictException extends RuntimeException {
  private final Map<String, String> fieldErrors;

  public RegistrationConflictException(String message, Map<String, String> fieldErrors) {
    super(message);
    this.fieldErrors = fieldErrors;
  }
}
