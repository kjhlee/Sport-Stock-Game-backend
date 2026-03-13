package com.sportstock.common.security;

import com.sportstock.common.exceptions.InvalidTokenException;
import com.sportstock.common.exceptions.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtValidationService {

  @Value("${jwt.secret}")
  private String SECRET_KEY;

  public Claims extractClaims(String token) {
    return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
  }

  public Claims validateAccessToken(String token) {
    try {
      return extractClaims(token);
    } catch (ExpiredJwtException e) {
      throw new TokenExpiredException(e.getMessage(), token);
    } catch (Exception e) {
      throw new InvalidTokenException(e.getMessage(), token);
    }
  }

  private SecretKey getSignInKey() {
    byte[] keyBytes = SECRET_KEY.getBytes();
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
