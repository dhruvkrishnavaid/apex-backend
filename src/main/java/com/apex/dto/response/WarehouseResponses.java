package com.apex.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public class WarehouseResponses {

  public record WarehouseResponse(
      UUID id,
      String code,
      String name,
      String addressLine,
      String city,
      String state,
      String postalCode,
      String country,
      String contactPhone,
      Boolean isActive,
      LocalDateTime createdAt) {
  }

  public record LocationResponse(
      UUID id,
      UUID warehouseId,
      String code,
      String description,
      Boolean isActive) {
  }
}
