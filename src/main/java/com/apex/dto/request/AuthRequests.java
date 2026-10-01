package com.apex.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class AuthRequests {

  public record LoginRequest(
      @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
      @NotBlank(message = "Password is required") String password) {
  }

  public record RegisterRequest(
      @NotBlank(message = "First name is required") String firstName,
      @NotBlank(message = "Last name is required") String lastName,
      @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
      @NotBlank(message = "Password is required") @Size(min = 8, message = "Password must be at least 8 characters long") String password,
      @NotNull(message = "Role ID is required") UUID roleId) {
  }

  public record RefreshTokenRequest(
      @NotBlank(message = "Refresh token is required") String refreshToken) {
  }
}
