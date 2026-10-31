package com.apex.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.SupplierRequests.CreateSupplierRequest;
import com.apex.dto.request.SupplierRequests.SupplierProductRequest;
import com.apex.dto.request.SupplierRequests.UpdateSupplierRequest;
import com.apex.dto.response.SupplierResponses.SupplierProductResponse;
import com.apex.dto.response.SupplierResponses.SupplierResponse;
import com.apex.service.SupplierService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

  private final SupplierService supplierService;

  @PostMapping
  @PreAuthorize("hasAuthority('supplier:create')")
  public ResponseEntity<SupplierResponse> create(@Valid @RequestBody CreateSupplierRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(request));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('supplier:read')")
  public ResponseEntity<Page<SupplierResponse>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Boolean active,
      @PageableDefault(size = 20, sort = "name") Pageable pageable) {
    return ResponseEntity.ok(supplierService.list(keyword, active, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('supplier:read')")
  public ResponseEntity<SupplierResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(supplierService.get(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('supplier:update')")
  public ResponseEntity<SupplierResponse> update(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateSupplierRequest request) {
    return ResponseEntity.ok(supplierService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('supplier:delete')")
  public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
    supplierService.deactivate(id);
    return ResponseEntity.noContent().build();
  }

  // ---------- Supplier catalogue ----------

  @PostMapping("/{id}/products")
  @PreAuthorize("hasAuthority('supplier:update')")
  public ResponseEntity<SupplierProductResponse> addProduct(
      @PathVariable UUID id,
      @Valid @RequestBody SupplierProductRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.addProduct(id, request));
  }

  @GetMapping("/{id}/products")
  @PreAuthorize("hasAuthority('supplier:read')")
  public ResponseEntity<List<SupplierProductResponse>> listProducts(@PathVariable UUID id) {
    return ResponseEntity.ok(supplierService.listProducts(id));
  }

  @PutMapping("/{id}/products/{variantId}")
  @PreAuthorize("hasAuthority('supplier:update')")
  public ResponseEntity<SupplierProductResponse> updateProduct(
      @PathVariable UUID id,
      @PathVariable UUID variantId,
      @Valid @RequestBody SupplierProductRequest request) {
    return ResponseEntity.ok(supplierService.updateProduct(id, variantId, request));
  }

  @DeleteMapping("/{id}/products/{variantId}")
  @PreAuthorize("hasAuthority('supplier:update')")
  public ResponseEntity<Void> removeProduct(@PathVariable UUID id, @PathVariable UUID variantId) {
    supplierService.removeProduct(id, variantId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/by-variant/{variantId}")
  @PreAuthorize("hasAuthority('supplier:read')")
  public ResponseEntity<List<SupplierProductResponse>> suppliersForVariant(@PathVariable UUID variantId) {
    return ResponseEntity.ok(supplierService.listSuppliersForVariant(variantId));
  }
}
