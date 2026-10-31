package com.apex.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class InventoryResponses {

  public record StockLevelResponse(
      UUID id,
      UUID variantId,
      String sku,
      String productName,
      UUID warehouseId,
      String warehouseCode,
      UUID locationId,
      String locationCode,
      int quantityOnHand,
      int quantityReserved,
      int quantityAvailable,
      LocalDateTime updatedAt) {
  }

  public record VariantStockSummary(
      UUID variantId,
      String sku,
      String productName,
      long totalOnHand,
      long totalReserved,
      long totalAvailable,
      List<StockLevelResponse> warehouses) {
  }

  public record MovementResponse(
      UUID id,
      UUID variantId,
      String sku,
      UUID warehouseId,
      String warehouseCode,
      String type,
      int quantityChange,
      int quantityAfter,
      String referenceType,
      UUID referenceId,
      String reason,
      UUID performedBy,
      LocalDateTime createdAt) {
  }
}
