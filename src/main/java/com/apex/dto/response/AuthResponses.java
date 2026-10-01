package com.apex.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AuthResponses {

  public record AuthResponse(
      String accessToken,
      String refreshToken,
      String tokenType,
      long expiresIn,
      UserResponse user) {
    public AuthResponse(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
      this(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
  }

  public record UserResponse(
      UUID id,
      String firstName,
      String lastName,
      String email,
      String roleName,
      List<String> permissions,
      Boolean isActive,
      Boolean mfaEnabled,
      LocalDateTime createdAt) {
  }
}
