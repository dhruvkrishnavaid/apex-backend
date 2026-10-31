package com.apex.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WarehouseRequests {

  public record CreateWarehouseRequest(
      @NotBlank(message = "Warehouse code is required") @Size(max = 30) String code,
      @NotBlank(message = "Warehouse name is required") @Size(max = 150) String name,
      String addressLine,
      String city,
      String state,
      String postalCode,
      String country,
      String contactPhone) {
  }

  public record UpdateWarehouseRequest(
      @NotBlank(message = "Warehouse name is required") @Size(max = 150) String name,
      String addressLine,
      String city,
      String state,
      String postalCode,
      String country,
      String contactPhone,
      Boolean isActive) {
  }

  public record LocationRequest(
      @NotBlank(message = "Location code is required") @Size(max = 50) String code,
      @Size(max = 255) String description,
      Boolean isActive) {
  }
}
