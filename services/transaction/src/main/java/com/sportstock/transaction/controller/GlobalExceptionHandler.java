package com.sportstock.transaction.controller;

import com.sportstock.common.exceptions.MissingAuthenticationException;
import com.sportstock.transaction.exception.InsufficientFundsException;
import com.sportstock.transaction.exception.InvalidTradeRequestException;
import com.sportstock.transaction.exception.StockNotActiveException;
import com.sportstock.transaction.exception.TransactionAccessDeniedException;
import com.sportstock.transaction.exception.TransactionException;
import com.sportstock.transaction.exception.TransactionStateException;
import com.sportstock.transaction.exception.WalletAlreadyExistsException;
import com.sportstock.transaction.exception.WalletNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(WalletNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleWalletNotFound(WalletNotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage());
  }

  @ExceptionHandler(InsufficientFundsException.class)
  public ResponseEntity<Map<String, Object>> handleInsufficientFunds(
      InsufficientFundsException ex) {
    return build(HttpStatus.BAD_REQUEST, "INSUFFICIENT_FUNDS", ex.getMessage());
  }

  @ExceptionHandler(StockNotActiveException.class)
  public ResponseEntity<Map<String, Object>> handleStockNotActive(StockNotActiveException ex) {
    return build(HttpStatus.BAD_REQUEST, "STOCK_NOT_ACTIVE", ex.getMessage());
  }

  @ExceptionHandler(InvalidTradeRequestException.class)
  public ResponseEntity<Map<String, Object>> handleInvalidTradeRequest(
      InvalidTradeRequestException ex) {
    return build(HttpStatus.BAD_REQUEST, "INVALID_TRADE_REQUEST", ex.getMessage());
  }

  @ExceptionHandler(WalletAlreadyExistsException.class)
  public ResponseEntity<Map<String, Object>> handleWalletAlreadyExists(
      WalletAlreadyExistsException ex) {
    return build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
  }

  @ExceptionHandler(TransactionStateException.class)
  public ResponseEntity<Map<String, Object>> handleTransactionStateConflict(
      TransactionStateException ex) {
    return build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
  }

  @ExceptionHandler(TransactionAccessDeniedException.class)
  public ResponseEntity<Map<String, Object>> handleTransactionAccessDenied(
      TransactionAccessDeniedException ex) {
    return build(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex) {
    List<String> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.toList());
    return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", errors);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Map<String, Object>> handleConstraintViolation(
      ConstraintViolationException ex) {
    List<String> errors =
        ex.getConstraintViolations().stream()
            .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
            .collect(Collectors.toList());
    return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", errors);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, Object>> handleDataIntegrity(
      DataIntegrityViolationException ex) {
    log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
    return build(
        HttpStatus.CONFLICT,
        "CONFLICT",
        "Operation could not be completed due to a data conflict. Please try again.");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
    return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
  }

  @ExceptionHandler(MissingAuthenticationException.class)
  public ResponseEntity<Map<String, Object>> handleMissingAuthentication(
      MissingAuthenticationException ex) {
    return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage());
  }

  @ExceptionHandler(TransactionException.class)
  public ResponseEntity<Map<String, Object>> handleTransactionException(TransactionException ex) {
    log.error("Transaction error: {}", ex.getMessage(), ex);
    return build(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", ex.getMessage());
  }

  private ResponseEntity<Map<String, Object>> build(
      HttpStatus status, String code, Object message) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", Instant.now());
    body.put("status", status.value());
    body.put("code", code);
    body.put("message", message);
    return ResponseEntity.status(status).body(body);
  }
}
