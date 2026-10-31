package com.apex.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ReportResponses {

  public record StockValuationReport(
      long totalUnits,
      BigDecimal totalCostValue,
      BigDecimal totalRetailValue,
      List<StockValuationRow> rows) {
  }

  public record StockValuationRow(
      UUID variantId,
      String sku,
      String productName,
      UUID warehouseId,
      String warehouseCode,
      int quantityOnHand,
      BigDecimal unitCost,
      BigDecimal unitRetailPrice,
      BigDecimal costValue,
      BigDecimal retailValue) {
  }

  public record LowStockRow(
      UUID variantId,
      String sku,
      String productName,
      UUID warehouseId,
      String warehouseCode,
      int quantityOnHand,
      int quantityAvailable) {
  }

  public record SalesReport(
      LocalDate from,
      LocalDate to,
      long orders,
      long unitsSold,
      BigDecimal revenue,
      BigDecimal cost,
      BigDecimal grossProfit,
      BigDecimal refunds,
      BigDecimal netRevenue,
      List<SalesDay> days) {
  }

  public record SalesDay(LocalDate date, long orders, long units, BigDecimal revenue, BigDecimal grossProfit) {
  }

  public record TopProductRow(
      UUID variantId,
      String sku,
      String productName,
      long unitsSold,
      BigDecimal revenue) {
  }

  public record Dashboard(
      long activeProducts,
      long activeWarehouses,
      long totalUnitsOnHand,
      BigDecimal stockCostValue,
      long openAlerts,
      long pendingOrders,
      long ordersToShip,
      long openPurchaseOrders,
      long ordersToday,
      BigDecimal revenueToday,
      LocalDateTime generatedAt) {
  }

  public record AuditLogResponse(
      UUID id,
      UUID userId,
      String userEmail,
      String method,
      String path,
      String resource,
      int statusCode,
      String ipAddress,
      LocalDateTime createdAt) {
  }
}
