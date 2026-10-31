package com.apex.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PurchaseOrderRequests {

  public record CreatePurchaseOrderRequest(
      @NotNull(message = "Supplier is required") UUID supplierId,
      @NotNull(message = "Destination warehouse is required") UUID warehouseId,
      LocalDate expectedDate,
      @Size(max = 1000) String notes,
      @NotEmpty(message = "At least one line is required") List<@Valid PoLineRequest> lines) {
  }

  public record UpdatePurchaseOrderRequest(
      @NotNull(message = "Destination warehouse is required") UUID warehouseId,
      LocalDate expectedDate,
      @Size(max = 1000) String notes,
      @NotEmpty(message = "At least one line is required") List<@Valid PoLineRequest> lines) {
  }

  /** When unitCost is omitted the supplier's catalogue price (or the variant cost price) is used. */
  public record PoLineRequest(
      @NotNull(message = "Variant ID is required") UUID variantId,
      @NotNull @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
      @DecimalMin(value = "0.0", inclusive = false, message = "Unit cost must be greater than 0") BigDecimal unitCost) {
  }

  public record ReceivePurchaseOrderRequest(
      @Size(max = 500) String notes,
      @NotEmpty(message = "At least one line is required") List<@Valid ReceiveLine> lines) {
  }

  public record ReceiveLine(
      @NotNull UUID lineId,
      @NotNull @Min(value = 1, message = "Quantity must be at least 1") Integer quantity) {
  }
}
