package com.sportstock.common.dto.user_authentication;

import lombok.Data;

@Data
public class RegisterRequest {
  private String email;
  private String username;
  private String firstName;
  private String lastName;
  private String password;
}
