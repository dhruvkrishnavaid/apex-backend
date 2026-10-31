package com.apex.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PurchaseOrderResponses {

  public record PurchaseOrderResponse(
      UUID id,
      String poNumber,
      UUID supplierId,
      String supplierName,
      UUID warehouseId,
      String warehouseCode,
      String status,
      LocalDate expectedDate,
      String notes,
      BigDecimal totalAmount,
      List<PoLineResponse> lines,
      LocalDateTime orderedAt,
      LocalDateTime createdAt) {
  }

  public record PoLineResponse(
      UUID id,
      UUID variantId,
      String sku,
      String productName,
      int quantityOrdered,
      int quantityReceived,
      int quantityOutstanding,
      BigDecimal unitCost,
      BigDecimal lineTotal) {
  }

  public record GoodsReceiptResponse(
      UUID id,
      String receiptNumber,
      String notes,
      List<ReceiptLineResponse> lines,
      UUID receivedBy,
      LocalDateTime receivedAt) {
  }

  public record ReceiptLineResponse(UUID poLineId, String sku, int quantity) {
  }
}
