package com.apex.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RoleRequests {

  public record CreateRoleRequest(
      @NotBlank(message = "Role name is required") @Size(max = 100) String name,
      @NotNull(message = "Permissions are required") List<String> permissions) {
  }

  public record UpdateRoleRequest(
      @NotBlank(message = "Role name is required") @Size(max = 100) String name,
      @NotNull(message = "Permissions are required") List<String> permissions) {
  }
}
