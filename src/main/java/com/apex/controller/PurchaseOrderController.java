package com.apex.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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

import com.apex.dto.request.PurchaseOrderRequests.CreatePurchaseOrderRequest;
import com.apex.dto.request.PurchaseOrderRequests.ReceivePurchaseOrderRequest;
import com.apex.dto.request.PurchaseOrderRequests.UpdatePurchaseOrderRequest;
import com.apex.dto.response.PurchaseOrderResponses.GoodsReceiptResponse;
import com.apex.dto.response.PurchaseOrderResponses.PurchaseOrderResponse;
import com.apex.model.PurchaseOrder.Status;
import com.apex.service.PurchaseOrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

  private final PurchaseOrderService service;

  @PostMapping
  @PreAuthorize("hasAuthority('purchase:create')")
  public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody CreatePurchaseOrderRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('purchase:read')")
  public ResponseEntity<Page<PurchaseOrderResponse>> list(
      @RequestParam(required = false) Status status,
      @RequestParam(required = false) UUID supplierId,
      @RequestParam(required = false) UUID warehouseId,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(service.list(status, supplierId, warehouseId, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('purchase:read')")
  public ResponseEntity<PurchaseOrderResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(service.get(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('purchase:update')")
  public ResponseEntity<PurchaseOrderResponse> update(
      @PathVariable UUID id,
      @Valid @RequestBody UpdatePurchaseOrderRequest request) {
    return ResponseEntity.ok(service.update(id, request));
  }

  @PostMapping("/{id}/submit")
  @PreAuthorize("hasAuthority('purchase:update')")
  public ResponseEntity<PurchaseOrderResponse> submit(@PathVariable UUID id) {
    return ResponseEntity.ok(service.submit(id));
  }

  @PostMapping("/{id}/receive")
  @PreAuthorize("hasAuthority('purchase:receive')")
  public ResponseEntity<GoodsReceiptResponse> receive(
      @PathVariable UUID id,
      @Valid @RequestBody ReceivePurchaseOrderRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.receive(id, request));
  }

  @GetMapping("/{id}/receipts")
  @PreAuthorize("hasAuthority('purchase:read')")
  public ResponseEntity<List<GoodsReceiptResponse>> receipts(@PathVariable UUID id) {
    return ResponseEntity.ok(service.listReceipts(id));
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('purchase:cancel')")
  public ResponseEntity<PurchaseOrderResponse> cancel(@PathVariable UUID id) {
    return ResponseEntity.ok(service.cancel(id));
  }

  @PostMapping("/{id}/close")
  @PreAuthorize("hasAuthority('purchase:update')")
  public ResponseEntity<PurchaseOrderResponse> close(@PathVariable UUID id) {
    return ResponseEntity.ok(service.close(id));
  }
}
