package com.apex.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class TransferResponses {

  public record TransferResponse(
      UUID id,
      String referenceNumber,
      UUID fromWarehouseId,
      String fromWarehouseCode,
      UUID toWarehouseId,
      String toWarehouseCode,
      String status,
      String notes,
      List<TransferLineResponse> lines,
      LocalDateTime dispatchedAt,
      LocalDateTime receivedAt,
      LocalDateTime createdAt) {
  }

  public record TransferLineResponse(
      UUID id,
      UUID variantId,
      String sku,
      int quantity,
      Integer receivedQuantity) {
  }
}
