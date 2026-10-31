package com.apex.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OrderResponses {

  public record CustomerResponse(
      UUID id,
      String name,
      String email,
      String phone,
      String addressLine,
      String city,
      String state,
      String postalCode,
      String country,
      String notes,
      Boolean isActive,
      LocalDateTime createdAt) {
  }

  public record OrderResponse(
      UUID id,
      String orderNumber,
      UUID customerId,
      String customerName,
      UUID warehouseId,
      String warehouseCode,
      String status,
      String channel,
      String shippingAddress,
      String notes,
      BigDecimal subtotal,
      BigDecimal taxAmount,
      BigDecimal total,
      String carrier,
      String trackingNumber,
      List<OrderLineResponse> lines,
      LocalDateTime shippedAt,
      LocalDateTime deliveredAt,
      LocalDateTime createdAt) {
  }

  public record OrderLineResponse(
      UUID id,
      UUID variantId,
      String sku,
      String productName,
      int quantity,
      int quantityPicked,
      int quantityReturned,
      BigDecimal unitPrice,
      BigDecimal discountPercent,
      BigDecimal lineTotal) {
  }

  public record ReturnResponse(
      UUID id,
      String returnNumber,
      UUID orderId,
      String orderNumber,
      String reason,
      boolean restocked,
      BigDecimal refundAmount,
      List<ReturnLineResponse> lines,
      LocalDateTime createdAt) {
  }

  public record ReturnLineResponse(UUID orderLineId, String sku, int quantity, BigDecimal refundAmount) {
  }
}
