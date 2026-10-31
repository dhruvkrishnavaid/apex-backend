package com.apex.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SupplierRequests {

  public record CreateSupplierRequest(
      @NotBlank(message = "Supplier code is required") @Size(max = 30) String code,
      @NotBlank(message = "Supplier name is required") @Size(max = 200) String name,
      String contactName,
      @Email(message = "Invalid email format") String email,
      String phone,
      String addressLine,
      String city,
      String country,
      String paymentTerms,
      @Min(value = 0, message = "Lead time cannot be negative") Integer leadTimeDays,
      @Size(max = 1000) String notes) {
  }

  public record UpdateSupplierRequest(
      @NotBlank(message = "Supplier name is required") @Size(max = 200) String name,
      String contactName,
      @Email(message = "Invalid email format") String email,
      String phone,
      String addressLine,
      String city,
      String country,
      String paymentTerms,
      @Min(value = 0, message = "Lead time cannot be negative") Integer leadTimeDays,
      @Size(max = 1000) String notes,
      Boolean isActive) {
  }

  public record SupplierProductRequest(
      @NotNull(message = "Variant ID is required") UUID variantId,
      String supplierSku,
      @NotNull(message = "Unit cost is required") @DecimalMin(value = "0.0", inclusive = false, message = "Unit cost must be greater than 0") BigDecimal unitCost,
      @Min(value = 1, message = "Minimum order quantity must be at least 1") Integer minOrderQuantity,
      Boolean isPreferred) {
  }
}
