package com.apex.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.response.ReportResponses.AuditLogResponse;
import com.apex.dto.response.ReportResponses.Dashboard;
import com.apex.dto.response.ReportResponses.LowStockRow;
import com.apex.dto.response.ReportResponses.SalesReport;
import com.apex.dto.response.ReportResponses.StockValuationReport;
import com.apex.dto.response.ReportResponses.TopProductRow;
import com.apex.service.ReportService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {

  private final ReportService reportService;

  @GetMapping("/dashboard")
  @PreAuthorize("hasAuthority('report:read')")
  public ResponseEntity<Dashboard> dashboard() {
    return ResponseEntity.ok(reportService.dashboard());
  }

  @GetMapping("/reports/stock-valuation")
  @PreAuthorize("hasAuthority('report:read')")
  public ResponseEntity<StockValuationReport> stockValuation(@RequestParam(required = false) UUID warehouseId) {
    return ResponseEntity.ok(reportService.stockValuation(warehouseId));
  }

  @GetMapping("/reports/low-stock")
  @PreAuthorize("hasAuthority('report:read')")
  public ResponseEntity<List<LowStockRow>> lowStock(@RequestParam(defaultValue = "10") int threshold) {
    return ResponseEntity.ok(reportService.lowStock(threshold));
  }

  /** Revenue is recognised when an order ships; defaults to the last 30 days. */
  @GetMapping("/reports/sales")
  @PreAuthorize("hasAuthority('report:read')")
  public ResponseEntity<SalesReport> sales(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ResponseEntity.ok(reportService.sales(from, to));
  }

  @GetMapping("/reports/top-products")
  @PreAuthorize("hasAuthority('report:read')")
  public ResponseEntity<List<TopProductRow>> topProducts(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "10") int limit) {
    return ResponseEntity.ok(reportService.topProducts(from, to, limit));
  }

  @GetMapping("/audit-logs")
  @PreAuthorize("hasAuthority('audit:read')")
  public ResponseEntity<Page<AuditLogResponse>> auditLogs(
      @RequestParam(required = false) UUID userId,
      @RequestParam(required = false) String method,
      @RequestParam(required = false) String resource,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(reportService.auditLogs(userId, method, resource, from, to, pageable));
  }
}
