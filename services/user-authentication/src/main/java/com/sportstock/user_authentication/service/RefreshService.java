package com.sportstock.user_authentication.service;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RefreshService {

  @Autowired JwtService jwtService;

  public String refreshAccessToken(String token) {
    Claims claims = jwtService.validateRefreshToken(token);
    Long userId = claims.get("userId", Long.class);
    String username = claims.get("username", String.class);
    return jwtService.generateAccessToken(claims.getSubject(), userId, username);
  }
}
