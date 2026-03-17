package com.sportstock.common.dto.user_authentication;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class TokenResponse {
  private final String accessToken;
  private final String refreshToken;
}
