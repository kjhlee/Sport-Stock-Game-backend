package com.sportstock.common.security;

import com.sportstock.common.exceptions.MissingAuthenticationException;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class RequestContextAuthorizationHeaderResolver {

  private RequestContextAuthorizationHeaderResolver() {}

  public static String resolveBearerAuthorizationHeader() {
    ServletRequestAttributes attrs =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs == null || attrs.getRequest() == null) {
      throw new MissingAuthenticationException("No request context available");
    }
    String authorizationHeader = attrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
    if (authorizationHeader == null || authorizationHeader.isBlank()) {
      throw new MissingAuthenticationException("Missing Authorization header on incoming request");
    }
    if (!authorizationHeader.startsWith("Bearer ")) {
      throw new MissingAuthenticationException(
          "Authorization header must use Bearer token format");
    }
    return authorizationHeader;
  }
}
