package com.apex.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class AlertResponses {

  public record ReorderRuleResponse(
      UUID id,
      UUID variantId,
      String sku,
      String productName,
      UUID warehouseId,
      String warehouseCode,
      int reorderPoint,
      int reorderQuantity,
      boolean isActive) {
  }

  public record AlertResponse(
      UUID id,
      UUID variantId,
      String sku,
      String productName,
      UUID warehouseId,
      String warehouseCode,
      String type,
      String status,
      int availableQuantity,
      int reorderPoint,
      LocalDateTime createdAt,
      LocalDateTime acknowledgedAt,
      LocalDateTime resolvedAt) {
  }

  public record AlertSummary(long open, long acknowledged, long resolved) {
  }

  public record ReplenishmentSuggestion(
      UUID variantId,
      String sku,
      String productName,
      UUID warehouseId,
      String warehouseCode,
      int availableQuantity,
      int reorderPoint,
      int suggestedQuantity,
      UUID preferredSupplierId,
      String preferredSupplierName,
      BigDecimal estimatedUnitCost) {
  }
}
