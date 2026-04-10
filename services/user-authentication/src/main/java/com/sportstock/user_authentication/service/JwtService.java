package com.sportstock.user_authentication.service;

import com.sportstock.common.exceptions.InvalidTokenException;
import com.sportstock.common.exceptions.RefreshTokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  @Value("${jwt.secret}")
  private String SECRET_KEY;

  @Value("${jwt.access.expire-interval}")
  private long accessExpireInterval;

  @Value("${jwt.refresh.expire-interval}")
  private long refreshExpireInterval;

  public String generateAccessToken(String email, Long id, String username) {
    return Jwts.builder()
        .subject(email)
        .claim("userId", id)
        .claim("username", username)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + this.accessExpireInterval))
        .signWith(getSignInKey())
        .compact();
  }

  public String generateRefreshToken(String email, Long id, String username) {
    return Jwts.builder()
        .subject(email)
        .claim("userId", id)
        .claim("username", username)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + this.refreshExpireInterval))
        .signWith(getSignInKey())
        .compact();
  }

  public String generateExpiredToken(String email) {
    return Jwts.builder()
        .subject(email)
        .issuedAt(new Date(System.currentTimeMillis() - 6000000))
        .expiration(new Date(System.currentTimeMillis() - 300000))
        .signWith(getSignInKey())
        .compact();
  }

  // THIS WILL ALL SIT IN COMMON SECURITY FOR THE FILTER
  public Claims extractClaims(String token) {
    return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
  }

  public Claims validateRefreshToken(String token) {
    try {
      return extractClaims(token);
    } catch (ExpiredJwtException e) {
      throw new RefreshTokenExpiredException(e.getMessage(), token);
    } catch (InvalidTokenException e) {
      throw new InvalidTokenException(e.getMessage(), token);
    }
  }

  private SecretKey getSignInKey() {
    byte[] keyBytes = SECRET_KEY.getBytes();
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
