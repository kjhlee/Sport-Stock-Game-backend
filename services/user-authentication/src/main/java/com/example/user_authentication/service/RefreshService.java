package com.example.user_authentication.service;

import com.example.user_authentication.security.exceptions.TokenExpiredException;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RefreshService {

  @Autowired JwtService jwtService;

  public String refreshAccessToken(String token) {
    try {
      Claims claims = jwtService.validateRefreshToken(token);
      return jwtService.generateAccessToken(claims.getSubject());
    } catch (TokenExpiredException e) {
      throw new TokenExpiredException(e.getMessage());
    }
  }
}
