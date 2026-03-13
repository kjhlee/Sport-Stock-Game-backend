package com.sportstock.user_authentication.service;

import com.sportstock.common.exceptions.RefreshTokenExpiredException;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RefreshService {

  @Autowired JwtService jwtService;

  public String refreshAccessToken(String token) throws Exception {
    try {
      Claims claims = jwtService.validateRefreshToken(token);
      return jwtService.generateAccessToken(claims.getSubject());
    } catch (RefreshTokenExpiredException e) {
      throw new Exception(e.getMessage());
    }
  }
}
