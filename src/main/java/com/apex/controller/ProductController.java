package com.apex.controller;

import com.apex.dto.request.ProductRequests.CreateBarcodeRequest;
import com.apex.dto.request.ProductRequests.CreateProductRequest;
import com.apex.dto.request.ProductRequests.CreateMediaRequest;
import com.apex.dto.request.ProductRequests.CreateVariantRequest;
import com.apex.dto.request.ProductRequests.UpdateProductRequest;
import com.apex.dto.request.ProductRequests.UpdateVariantRequest;
import com.apex.dto.response.ProductResponses.BarcodeResponse;
import com.apex.dto.response.ProductResponses.MediaResponse;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  // ---------- Products ----------

  @PostMapping
  @PreAuthorize("hasAuthority('product:create')")
  public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('product:read')")
  public ResponseEntity<Page<ProductResponse>> listProducts(
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) String keyword,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(productService.listProducts(categoryId, keyword, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('product:read')")
  public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
    return ResponseEntity.ok(productService.getProduct(id));
  }

  @GetMapping("/slug/{slug}")
  @PreAuthorize("hasAuthority('product:read')")
  public ResponseEntity<ProductResponse> getProductBySlug(@PathVariable String slug) {
    return ResponseEntity.ok(productService.getProductBySlug(slug));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('product:update')")
  public ResponseEntity<ProductResponse> updateProduct(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateProductRequest request) {
    return ResponseEntity.ok(productService.updateProduct(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('product:delete')")
  public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
    productService.deleteProduct(id);
    return ResponseEntity.noContent().build();
  }

  // ---------- Variants ----------

  @PostMapping("/{productId}/variants")
  @PreAuthorize("hasAuthority('product:update')")
  public ResponseEntity<VariantResponse> addVariant(
      @PathVariable UUID productId,
      @Valid @RequestBody CreateVariantRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.addVariant(productId, request));
  }

  @GetMapping("/{productId}/variants")
  @PreAuthorize("hasAuthority('product:read')")
  public ResponseEntity<Page<VariantResponse>> listVariants(
      @PathVariable UUID productId,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(productService.listVariants(productId, pageable));
  }

  @GetMapping("/variants/sku/{sku}")
  @PreAuthorize("hasAuthority('product:read')")
  public ResponseEntity<VariantResponse> getVariantBySku(@PathVariable String sku) {
    return ResponseEntity.ok(productService.getVariantBySku(sku));
  }

  @PutMapping("/variants/{variantId}")
  @PreAuthorize("hasAuthority('product:update')")
  public ResponseEntity<VariantResponse> updateVariant(
      @PathVariable UUID variantId,
      @Valid @RequestBody UpdateVariantRequest request) {
    return ResponseEntity.ok(productService.updateVariant(variantId, request));
  }

  @DeleteMapping("/variants/{variantId}")
  @PreAuthorize("hasAuthority('product:delete')")
  public ResponseEntity<Void> deactivateVariant(@PathVariable UUID variantId) {
    productService.deactivateVariant(variantId);
    return ResponseEntity.noContent().build();
  }

  // ---------- Media ----------

  @PostMapping("/{productId}/media")
  @PreAuthorize("hasAuthority('product:update')")
  public ResponseEntity<MediaResponse> addMedia(
      @PathVariable UUID productId,
      @Valid @RequestBody CreateMediaRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.addMedia(productId, request));
  }

  @GetMapping("/{productId}/media")
  @PreAuthorize("hasAuthority('product:read')")
  public ResponseEntity<List<MediaResponse>> listMedia(@PathVariable UUID productId) {
    return ResponseEntity.ok(productService.listMedia(productId));
  }

  @DeleteMapping("/media/{mediaId}")
  @PreAuthorize("hasAuthority('product:update')")
  public ResponseEntity<Void> deleteMedia(@PathVariable UUID mediaId) {
    productService.deleteMedia(mediaId);
    return ResponseEntity.noContent().build();
  }

  // ---------- Barcodes & Hardware Scanners ----------

  @DeleteMapping("/barcodes/id/{barcodeId}")
  @PreAuthorize("hasAuthority('product:update')")
  public ResponseEntity<Void> removeBarcode(@PathVariable UUID barcodeId) {
    productService.removeBarcode(barcodeId);
    return ResponseEntity.noContent().build();
  }


  @PostMapping("/variants/{variantId}/barcodes")
  @PreAuthorize("hasAuthority('product:update')")
  public ResponseEntity<BarcodeResponse> addBarcode(
      @PathVariable UUID variantId,
      @Valid @RequestBody CreateBarcodeRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.addBarcode(variantId, request));
  }

  @GetMapping("/barcodes/{barcode}")
  @PreAuthorize("hasAuthority('product:read')")
  public ResponseEntity<VariantResponse> lookupByBarcode(@PathVariable String barcode) {
    return ResponseEntity.ok(productService.lookupByBarcode(barcode));
  }
}
