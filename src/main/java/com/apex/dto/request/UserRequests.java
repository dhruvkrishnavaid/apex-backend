package com.apex.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UserRequests {

  public record UpdateUserRequest(
      @NotBlank(message = "First name is required") String firstName,
      @NotBlank(message = "Last name is required") String lastName,
      @NotNull(message = "Role ID is required") UUID roleId,
      Boolean isActive) {
  }

  public record ChangePasswordRequest(
      @NotBlank(message = "Current password is required") String currentPassword,
      @NotBlank(message = "New password is required") @Size(min = 8, message = "Password must be at least 8 characters long") String newPassword) {
  }

  public record ResetPasswordRequest(
      @NotBlank(message = "New password is required") @Size(min = 8, message = "Password must be at least 8 characters long") String newPassword) {
  }
}
