package com.apex.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProductRequests {

  public record CreateProductRequest(
      @NotNull(message = "Category ID is required") UUID categoryId,
      @NotBlank(message = "Product name is required") String name,
      String description,
      String brand,
      @NotBlank(message = "Unit of measure is required") String unitOfMeasure,
      Boolean hasVariants,
      List<CreateVariantRequest> variants) {
  }

  public record CreateVariantRequest(
      @NotBlank(message = "SKU is required") String sku,
      Map<String, Object> variantAttributes,
      @NotNull(message = "Cost price is required") @DecimalMin(value = "0.0", inclusive = false, message = "Cost price must be greater than 0") BigDecimal costPrice,
      @NotNull(message = "Retail price is required") @DecimalMin(value = "0.0", inclusive = false, message = "Retail price must be greater than 0") BigDecimal retailPrice,
      Map<String, Object> dimensions,
      List<CreateBarcodeRequest> barcodes) {
  }

  public record CreateBarcodeRequest(
      @NotBlank(message = "Barcode string is required") String barcode,
      @NotBlank(message = "Barcode type is required") String type,
      Boolean isPrimary) {
  }
}
