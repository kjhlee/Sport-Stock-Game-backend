package com.example.user_authentication.security;

import com.example.user_authentication.security.exceptions.InvalidTokenException;
import com.example.user_authentication.security.exceptions.TokenExpiredException;
import com.example.user_authentication.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  @Autowired JwtService jwtService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }
    String token = authHeader.substring(7);
    try {
      Claims claims = jwtService.validateAccessToken(token);
      String subject = claims.getSubject();

      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(subject, null, Collections.emptyList());

      authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

      SecurityContextHolder.getContext().setAuthentication(authentication);
    } catch (TokenExpiredException e) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.getWriter().write("TOKEN_EXPIRED");
      return;
    } catch (InvalidTokenException e) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.getWriter().write("INVALID_TOKEN");
      return;
    }
    filterChain.doFilter(request, response);
  }
}
