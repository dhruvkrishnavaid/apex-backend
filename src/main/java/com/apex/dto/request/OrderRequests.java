package com.apex.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.apex.model.SalesOrder.Channel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class OrderRequests {

  public record CreateOrderRequest(
      UUID customerId,
      @NotNull(message = "Fulfilment warehouse is required") UUID warehouseId,
      Channel channel,
      @Size(max = 500) String shippingAddress,
      @Size(max = 1000) String notes,
      @NotEmpty(message = "At least one line is required") List<@Valid OrderLineRequest> lines) {
  }

  /** unitPrice defaults to the variant's retail price. */
  public record OrderLineRequest(
      @NotNull(message = "Variant ID is required") UUID variantId,
      @NotNull @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
      @DecimalMin(value = "0.0", inclusive = false, message = "Unit price must be greater than 0") BigDecimal unitPrice,
      @DecimalMin(value = "0.0") @DecimalMax(value = "100.0") BigDecimal discountPercent) {
  }

  /** Lines left out keep their current picked quantity; an empty body picks the whole order. */
  public record PickRequest(List<@Valid PickLine> lines) {
    public record PickLine(
        @NotNull UUID lineId,
        @NotNull @Min(value = 0, message = "Quantity cannot be negative") Integer quantity) {
    }
  }

  public record ShipOrderRequest(
      @Size(max = 100) String carrier,
      @Size(max = 100) String trackingNumber) {
  }

  public record CancelOrderRequest(@Size(max = 500) String reason) {
  }

  public record CreateReturnRequest(
      @Size(max = 500) String reason,
      Boolean restock,
      @NotEmpty(message = "At least one line is required") List<@Valid ReturnLineRequest> lines) {
  }

  public record ReturnLineRequest(
      @NotNull UUID orderLineId,
      @NotNull @Min(value = 1, message = "Quantity must be at least 1") Integer quantity) {
  }
}
