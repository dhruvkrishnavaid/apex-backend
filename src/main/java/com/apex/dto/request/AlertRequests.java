package com.apex.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AlertRequests {

  public record ReorderRuleRequest(
      @NotNull(message = "Variant ID is required") UUID variantId,
      @NotNull(message = "Warehouse ID is required") UUID warehouseId,
      @NotNull @Min(value = 0, message = "Reorder point cannot be negative") Integer reorderPoint,
      @NotNull @Min(value = 1, message = "Reorder quantity must be at least 1") Integer reorderQuantity,
      Boolean isActive) {
  }
}
