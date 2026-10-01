package com.apex.controller;

import com.apex.dto.request.ProductRequests.CreateBarcodeRequest;
import com.apex.dto.request.ProductRequests.CreateProductRequest;
import com.apex.dto.request.ProductRequests.CreateVariantRequest;
import com.apex.dto.response.ProductResponses.BarcodeResponse;
import com.apex.dto.response.ProductResponses.ProductResponse;
import com.apex.dto.response.ProductResponses.VariantResponse;
import com.apex.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  // ---------- Products ----------

  @PostMapping
  public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
  }

  @GetMapping
  public ResponseEntity<Page<ProductResponse>> listProducts(
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) String keyword,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(productService.listProducts(categoryId, keyword, pageable));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
    return ResponseEntity.ok(productService.getProduct(id));
  }

  @GetMapping("/slug/{slug}")
  public ResponseEntity<ProductResponse> getProductBySlug(@PathVariable String slug) {
    return ResponseEntity.ok(productService.getProductBySlug(slug));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
    productService.deleteProduct(id);
    return ResponseEntity.noContent().build();
  }

  // ---------- Variants ----------

  @PostMapping("/{productId}/variants")
  public ResponseEntity<VariantResponse> addVariant(
      @PathVariable UUID productId,
      @Valid @RequestBody CreateVariantRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.addVariant(productId, request));
  }

  @GetMapping("/{productId}/variants")
  public ResponseEntity<Page<VariantResponse>> listVariants(
      @PathVariable UUID productId,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(productService.listVariants(productId, pageable));
  }

  @GetMapping("/variants/sku/{sku}")
  public ResponseEntity<VariantResponse> getVariantBySku(@PathVariable String sku) {
    return ResponseEntity.ok(productService.getVariantBySku(sku));
  }

  // ---------- Barcodes & Hardware Scanners ----------

  @PostMapping("/variants/{variantId}/barcodes")
  public ResponseEntity<BarcodeResponse> addBarcode(
      @PathVariable UUID variantId,
      @Valid @RequestBody CreateBarcodeRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.addBarcode(variantId, request));
  }

  @GetMapping("/barcodes/{barcode}")
  public ResponseEntity<VariantResponse> lookupByBarcode(@PathVariable String barcode) {
    return ResponseEntity.ok(productService.lookupByBarcode(barcode));
  }
}
