package com.sportstock.user_authentication.controller;

import com.sportstock.common.dto.user_authentication.LoginRequest;
import com.sportstock.common.dto.user_authentication.RefreshRequest;
import com.sportstock.common.dto.user_authentication.RegisterRequest;
import com.sportstock.common.dto.user_authentication.TokenResponse;
import com.sportstock.user_authentication.service.LoginService;
import com.sportstock.user_authentication.service.RefreshService;
import com.sportstock.user_authentication.service.RegisterAccountService;
import lombok.RequiredArgsConstructor;
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

  private final RegisterAccountService accountService;
  private final LoginService loginService;
  private final RefreshService refreshService;

  @PostMapping("/register")
  public ResponseEntity<String> registerAccount(@RequestBody RegisterRequest request) {
    accountService.registerAccount(
        request.getEmail(),
        request.getPassword(),
        request.getUsername(),
        request.getFirstName(),
        request.getLastName());
    return ResponseEntity.ok("Account registered successfully");
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    TokenResponse newToken = loginService.login(request.getLogin(), request.getPassword());
    return ResponseEntity.ok(newToken);
  }

  @PostMapping("/refresh")
  public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
    String refreshToken = request.getRefreshToken();
    String newAccessToken = refreshService.refreshAccessToken(refreshToken);
    return ResponseEntity.ok(new TokenResponse(newAccessToken, refreshToken));
  }

  @GetMapping("/test")
  public String tester() {
    return "test";
  }
}
