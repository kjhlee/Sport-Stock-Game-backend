package com.sportstock.transaction.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class CurrentUserProvider {

  private static final String USER_ID_HEADER = "X-User-Id";

  // Right now gets user id from header but once oauth is implemented switch to read from
  // SecurityContext and add Spring Security dependency + config class (SecurityFilterChain bean)
  public Long getCurrentUserId() {
    ServletRequestAttributes attrs =
        (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    HttpServletRequest request = attrs.getRequest();
    String header = request.getHeader(USER_ID_HEADER);
    if (header == null || header.isBlank()) {
      throw new IllegalArgumentException("Missing required header: " + USER_ID_HEADER);
    }
    try {
      return Long.parseLong(header.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Invalid " + USER_ID_HEADER + " header: must be a numeric user ID");
    }
  }
}
