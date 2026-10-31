package com.apex.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class SupplierResponses {

  public record SupplierResponse(
      UUID id,
      String code,
      String name,
      String contactName,
      String email,
      String phone,
      String addressLine,
      String city,
      String country,
      String paymentTerms,
      Integer leadTimeDays,
      String notes,
      Boolean isActive,
      LocalDateTime createdAt) {
  }

  public record SupplierProductResponse(
      UUID id,
      UUID supplierId,
      String supplierName,
      UUID variantId,
      String sku,
      String productName,
      String supplierSku,
      BigDecimal unitCost,
      int minOrderQuantity,
      boolean isPreferred) {
  }
}
