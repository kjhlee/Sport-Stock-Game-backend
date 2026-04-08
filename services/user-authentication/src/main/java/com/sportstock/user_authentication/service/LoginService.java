package com.sportstock.user_authentication.service;

import com.sportstock.common.dto.user_authentication.TokenResponse;
import com.sportstock.user_authentication.models.UserDetails;
import com.sportstock.user_authentication.repository.UserAccountRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

  private static Logger logger = LoggerFactory.getLogger(LoginService.class);

  private final BCryptPasswordEncoder passwordEncoder;

  private final JwtService jwtService;

  private final UserAccountRepo accountRepo;

  public TokenResponse login(String login, String password) {
    UserDetails account;
    System.out.println("EXPIRED: " + jwtService.generateExpiredToken(login));

    if (login.contains("@")) {
      account =
          accountRepo.findByEmail(login).orElseThrow(() -> new RuntimeException("Email not found"));
    } else {
      account =
          accountRepo
              .findByUsername(login)
              .orElseThrow(() -> new RuntimeException("Username not found "));
    }
    logger.info("Login attempt for account: {}", account);
    if (!passwordEncoder.matches(password, account.getPassword())) {
      logger.error("Login failed for account: {}", account);
      throw new RuntimeException("Invalid credentials");
    }

    String accessToken = jwtService.generateAccessToken(account.getEmail(), account.getId(), account.getUsername());
    String refreshToken = jwtService.generateRefreshToken(account.getEmail(), account.getId(), account.getUsername());

    return new TokenResponse(accessToken, refreshToken);
  }
}
