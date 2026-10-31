package com.apex.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TransferRequests {

  public record CreateTransferRequest(
      @NotNull(message = "Source warehouse is required") UUID fromWarehouseId,
      @NotNull(message = "Destination warehouse is required") UUID toWarehouseId,
      @Size(max = 500) String notes,
      @NotEmpty(message = "At least one line is required") List<@Valid TransferLineRequest> lines) {
  }

  public record TransferLineRequest(
      @NotNull(message = "Variant ID is required") UUID variantId,
      @NotNull @Min(value = 1, message = "Quantity must be at least 1") Integer quantity) {
  }

  /** Optional per-line override; lines left out are received in full. */
  public record ReceiveTransferRequest(List<@Valid ReceivedLine> lines) {
  }

  public record ReceivedLine(
      @NotNull UUID lineId,
      @NotNull @Min(value = 0, message = "Received quantity cannot be negative") Integer receivedQuantity) {
  }
}
