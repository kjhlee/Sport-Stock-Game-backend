package com.sportstock.common.security;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

  private static final String USER_ID_QUERY = "SELECT id FROM users.userinfo WHERE email = ?";

  @Autowired DataSource dataSource;

  public Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getPrincipal() == null) {
      throw new IllegalStateException("No authenticated user found in security context");
    }

    String email = authentication.getPrincipal().toString();
    if (email == null || email.isBlank()) {
      throw new IllegalStateException("No user email found in security context");
    }

    String rawUserId = fetchUserIdByEmail(email);
    try {
      return Long.parseLong(rawUserId);
    } catch (NumberFormatException e) {
      throw new IllegalStateException(
          "User ID is not numeric and cannot be mapped to Long. email="
              + email
              + ", userId="
              + rawUserId,
          e);
    }
  }

  private String fetchUserIdByEmail(String email) {
    try {
      return queryForUserId(email, USER_ID_QUERY);
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to query user id for email: " + email, e);
    }
  }

  private String queryForUserId(String email, String query) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        PreparedStatement statement = connection.prepareStatement(query)) {
      statement.setString(1, email);
      try (ResultSet rs = statement.executeQuery()) {
        if (!rs.next()) {
          throw new IllegalStateException("No user found for email: " + email);
        }
        String userId = rs.getString("id");
        if (userId == null || userId.isBlank()) {
          throw new IllegalStateException("User id is null for email: " + email);
        }
        return userId;
      }
    }
  }
}
