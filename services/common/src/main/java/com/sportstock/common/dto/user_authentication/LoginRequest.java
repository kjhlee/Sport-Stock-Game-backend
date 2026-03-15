package com.sportstock.common.dto.user_authentication;

import lombok.Data;

@Data
public class LoginRequest {
  private String login;
  private String password;
}
