package com.apex.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProductResponses {

  public record ProductResponse(
      UUID id,
      UUID categoryId,
      String categoryName,
      String name,
      String slug,
      String description,
      String brand,
      String unitOfMeasure,
      Boolean hasVariants,
      Boolean isActive,
      List<VariantResponse> variants,
      List<String> mediaUrls,
      LocalDateTime createdAt) {
  }

  public record VariantResponse(
      UUID id,
      UUID productId,
      String sku,
      Map<String, Object> variantAttributes,
      BigDecimal costPrice,
      BigDecimal retailPrice,
      Map<String, Object> dimensions,
      Boolean isActive,
      List<BarcodeResponse> barcodes,
      LocalDateTime createdAt) {
  }

  public record BarcodeResponse(
      UUID id,
      String barcode,
      String type,
      Boolean isPrimary) {
  }
}
