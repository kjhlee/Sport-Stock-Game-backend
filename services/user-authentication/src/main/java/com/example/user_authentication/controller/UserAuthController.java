package com.example.user_authentication.controller;

import com.example.user_authentication.DTO.LoginRequest;
import com.example.user_authentication.DTO.RefreshRequest;
import com.example.user_authentication.DTO.RegisterRequest;
import com.example.user_authentication.DTO.TokenResponse;
import com.example.user_authentication.security.exceptions.TokenExpiredException;
import com.example.user_authentication.service.LoginService;
import com.example.user_authentication.service.RefreshService;
import com.example.user_authentication.service.RegisterAccountService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserAuthController {

  private static Logger logger = LoggerFactory.getLogger(UserAuthController.class);

  private final RegisterAccountService accountService;
  private final LoginService loginService;
  private final RefreshService refreshService;

  @PostMapping("/register")
  public ResponseEntity<String> registerAccount(@RequestBody RegisterRequest request) {
    logger.info("Processing new /api/register request");
    try {
      accountService.registerAccount(
          request.getEmail(),
          request.getPassword(),
          request.getUsername(),
          request.getFirstName(),
          request.getLastName());
      return ResponseEntity.ok("Account registered successfully");
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    logger.info("Processing new /api/login request");
    try {
      TokenResponse newToken = loginService.login(request.getLogin(), request.getPassword());
      return ResponseEntity.ok(newToken);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }

  // this is a test mapping if the user has valid token they can access this endpoint
  @GetMapping("/test")
  public ResponseEntity<String> test() {
    return ResponseEntity.ok("Test successful");
  }

  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
    try {
      logger.info("Processing new refresh token /api/refresh");
      String refreshToken = request.getRefreshToken();
      String newAccessToken = refreshService.refreshAccessToken(refreshToken);
      return ResponseEntity.ok(new TokenResponse(newAccessToken, refreshToken));
    } catch (TokenExpiredException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }
  }
}
