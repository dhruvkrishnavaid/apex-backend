package com.apex.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class InventoryRequests {

  /** Positive delta adds stock, negative removes it. */
  public record AdjustStockRequest(
      @NotNull(message = "Variant ID is required") UUID variantId,
      @NotNull(message = "Warehouse ID is required") UUID warehouseId,
      @NotNull(message = "Quantity change is required") Integer quantityChange,
      @NotBlank(message = "A reason is required") @Size(max = 500) String reason,
      Boolean damaged) {
  }

  /** Physical count: sets quantity on hand to the counted figure. */
  public record StockCountRequest(
      @NotNull(message = "Variant ID is required") UUID variantId,
      @NotNull(message = "Warehouse ID is required") UUID warehouseId,
      @NotNull(message = "Counted quantity is required") @Min(value = 0, message = "Counted quantity cannot be negative") Integer countedQuantity,
      @Size(max = 500) String reason) {
  }

  public record AssignLocationRequest(UUID locationId) {
  }
}
