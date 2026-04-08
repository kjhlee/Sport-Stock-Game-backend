package com.sportstock.common.security;

import com.sportstock.common.exceptions.MissingAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

  private AuthenticatedUser getPrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser)) {
      throw new MissingAuthenticationException("No authenticated user found in security context");
    }
    return (AuthenticatedUser) authentication.getPrincipal();
  }

  public Long getCurrentUserId() {
    return getPrincipal().userId();
  }

  public String getCurrentUserEmail() {
    return getPrincipal().email();
  }

  public String getCurrentUsername() {
    return getPrincipal().username();
  }
}
