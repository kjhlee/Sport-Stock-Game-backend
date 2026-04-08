package com.sportstock.common.dto.user_authentication;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthErrorResponse {
  private final Instant timestamp;
  private final int status;
  private final String code;
  private final String message;
  private final Map<String, String> fieldErrors;
}
