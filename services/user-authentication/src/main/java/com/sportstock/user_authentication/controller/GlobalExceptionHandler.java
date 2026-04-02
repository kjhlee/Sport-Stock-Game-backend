package com.sportstock.user_authentication.controller;

import com.sportstock.common.dto.user_authentication.AuthErrorResponse;
import com.sportstock.common.exceptions.InvalidTokenException;
import com.sportstock.common.exceptions.RefreshTokenExpiredException;
import com.sportstock.user_authentication.exception.InvalidCredentialsException;
import com.sportstock.user_authentication.exception.RegistrationConflictException;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(RegistrationConflictException.class)
  public ResponseEntity<AuthErrorResponse> handleRegistrationConflict(
      RegistrationConflictException ex) {
    return build(
        HttpStatus.CONFLICT, "REGISTRATION_CONFLICT", ex.getMessage(), ex.getFieldErrors());
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<AuthErrorResponse> handleInvalidCredentials(
      InvalidCredentialsException ex) {
    return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage(), null);
  }

  @ExceptionHandler(RefreshTokenExpiredException.class)
  public ResponseEntity<AuthErrorResponse> handleRefreshTokenExpired(
      RefreshTokenExpiredException ex) {
    return build(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", ex.getMessage(), null);
  }

  @ExceptionHandler(InvalidTokenException.class)
  public ResponseEntity<AuthErrorResponse> handleInvalidToken(InvalidTokenException ex) {
    return build(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", ex.getMessage(), null);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<AuthErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), null);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<AuthErrorResponse> handleUnexpected(Exception ex) {
    log.error("Unhandled auth exception", ex);
    return build(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_SERVER_ERROR",
        "Something went wrong. Please try again.",
        null);
  }

  private ResponseEntity<AuthErrorResponse> build(
      HttpStatus status, String code, String message, Map<String, String> fieldErrors) {
    return ResponseEntity.status(status)
        .body(
            AuthErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .code(code)
                .message(message)
                .fieldErrors(fieldErrors)
                .build());
  }
}
