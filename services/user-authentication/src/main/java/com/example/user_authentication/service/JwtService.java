package com.example.user_authentication.service;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.user_authentication.security.exceptions.InvalidTokenException;
import com.example.user_authentication.security.exceptions.TokenExpiredException;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.access.expire-interval}")
    private long accessExpireInterval;

    @Value("${jwt.refresh.expire-interval}")
    private long refreshExpireInterval;

    //TODO: fix the extraClaims to be more specific to the user, such as user id, email, etc.
    // public String generateToken(Optional<Map<String, String>> extraClaims, String email) {
    public String generateAccessToken(String email) {

        return Jwts
             .builder()
             .subject(email)
             .issuedAt(new Date(System.currentTimeMillis()))
             .expiration(new Date(System.currentTimeMillis() + this.accessExpireInterval))
             .signWith(getSignInKey())
             .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts
             .builder()
             .subject(email)
             .issuedAt(new Date(System.currentTimeMillis()))
             .expiration(new Date(System.currentTimeMillis() + this.refreshExpireInterval))
             .signWith(getSignInKey())
             .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
    }

    public Claims validateAccessToken(String token){
        try {
            return extractClaims(token);
        } catch (ExpiredJwtException e){
            throw new TokenExpiredException(e.getMessage());
        } catch (Exception e){
            throw new InvalidTokenException(e.getMessage());
        }
    }

    public Claims validateRefreshToken(String token){
        try {
            return extractClaims(token);
        } catch (ExpiredJwtException e) {
            throw new TokenExpiredException(token + e.getMessage());
        } catch (Exception e) {
            throw new InvalidTokenException(token + e.getMessage());
        }
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = SECRET_KEY.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
