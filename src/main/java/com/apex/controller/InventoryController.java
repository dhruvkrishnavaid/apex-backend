package com.apex.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.InventoryRequests.AdjustStockRequest;
import com.apex.dto.request.InventoryRequests.AssignLocationRequest;
import com.apex.dto.request.InventoryRequests.StockCountRequest;
import com.apex.dto.response.InventoryResponses.MovementResponse;
import com.apex.dto.response.InventoryResponses.StockLevelResponse;
import com.apex.dto.response.InventoryResponses.VariantStockSummary;
import com.apex.model.StockMovement.MovementType;
import com.apex.service.InventoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

  private final InventoryService inventoryService;

  @GetMapping
  @PreAuthorize("hasAuthority('inventory:read')")
  public ResponseEntity<Page<StockLevelResponse>> listStock(
      @RequestParam(required = false) UUID warehouseId,
      @RequestParam(required = false) UUID variantId,
      @RequestParam(defaultValue = "false") boolean inStockOnly,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(inventoryService.listStock(warehouseId, variantId, inStockOnly, pageable));
  }

  @GetMapping("/variants/{variantId}")
  @PreAuthorize("hasAuthority('inventory:read')")
  public ResponseEntity<VariantStockSummary> variantSummary(@PathVariable UUID variantId) {
    return ResponseEntity.ok(inventoryService.getVariantSummary(variantId));
  }

  @GetMapping("/movements")
  @PreAuthorize("hasAuthority('inventory:read')")
  public ResponseEntity<Page<MovementResponse>> listMovements(
      @RequestParam(required = false) UUID variantId,
      @RequestParam(required = false) UUID warehouseId,
      @RequestParam(required = false) MovementType type,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(inventoryService.listMovements(variantId, warehouseId, type, from, to, pageable));
  }

  @PostMapping("/adjustments")
  @PreAuthorize("hasAuthority('inventory:adjust')")
  public ResponseEntity<StockLevelResponse> adjust(@Valid @RequestBody AdjustStockRequest request) {
    return ResponseEntity.ok(inventoryService.adjust(request));
  }

  @PostMapping("/stock-counts")
  @PreAuthorize("hasAuthority('inventory:adjust')")
  public ResponseEntity<StockLevelResponse> stockCount(@Valid @RequestBody StockCountRequest request) {
    return ResponseEntity.ok(inventoryService.stockCount(request));
  }

  @PutMapping("/variants/{variantId}/warehouses/{warehouseId}/location")
  @PreAuthorize("hasAuthority('inventory:adjust')")
  public ResponseEntity<StockLevelResponse> assignLocation(
      @PathVariable UUID variantId,
      @PathVariable UUID warehouseId,
      @RequestBody AssignLocationRequest request) {
    return ResponseEntity.ok(inventoryService.assignLocation(variantId, warehouseId, request.locationId()));
  }
}
