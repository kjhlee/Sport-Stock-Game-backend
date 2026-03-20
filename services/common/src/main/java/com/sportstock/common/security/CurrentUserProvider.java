package com.sportstock.common.security;

import com.sportstock.common.exceptions.MissingAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

  public Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getPrincipal() == null) {
      throw new MissingAuthenticationException("No authenticated user found in security context");
    }

    AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
    Long userId = user.userId();

    if (userId == null) {
      throw new MissingAuthenticationException("No user id found in security context");
    }

    return userId;
  }

  public String getCurrentUserEmail() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getPrincipal() == null) {
      throw new MissingAuthenticationException("No authenticated user found in security context");
    }

    AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();

    String email = user.email();

    if (email == null || email.isBlank()) {
      throw new MissingAuthenticationException("No user email found in security context");
    }

    return email;
  }
}
