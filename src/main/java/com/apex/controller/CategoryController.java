package com.apex.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apex.dto.request.CategoryRequests.CreateCategoryRequest;
import com.apex.dto.request.CategoryRequests.MoveCategoryRequest;
import com.apex.dto.request.CategoryRequests.UpdateCategoryRequest;
import com.apex.dto.response.CategoryResponse;
import com.apex.service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;

  @PostMapping
  @PreAuthorize("hasAuthority('category:create')")
  public ResponseEntity<CategoryResponse> createCategory(
      @Valid @RequestBody CreateCategoryRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('category:read')")
  public ResponseEntity<List<CategoryResponse>> getCategoryTree() {
    return ResponseEntity.ok(categoryService.getCategoryTree());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('category:read')")
  public ResponseEntity<CategoryResponse> getCategory(@PathVariable UUID id) {
    return ResponseEntity.ok(categoryService.getCategory(id));
  }

  @GetMapping("/slug/{slug}")
  @PreAuthorize("hasAuthority('category:read')")
  public ResponseEntity<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
    return ResponseEntity.ok(categoryService.getCategoryBySlug(slug));
  }

  @GetMapping("/{parentId}/subcategories")
  @PreAuthorize("hasAuthority('category:read')")
  public ResponseEntity<List<CategoryResponse>> getSubcategories(@PathVariable UUID parentId) {
    return ResponseEntity.ok(categoryService.getSubcategories(parentId));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('category:update')")
  public ResponseEntity<CategoryResponse> updateCategory(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCategoryRequest request) {
    return ResponseEntity.ok(categoryService.updateCategory(id, request));
  }

  @PatchMapping("/{id}/parent")
  @PreAuthorize("hasAuthority('category:update')")
  public ResponseEntity<CategoryResponse> moveCategory(
      @PathVariable UUID id,
      @RequestBody MoveCategoryRequest request) {
    return ResponseEntity.ok(categoryService.moveCategory(id, request.parentId()));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('category:delete')")
  public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
    categoryService.deleteCategory(id);
    return ResponseEntity.noContent().build();
  }
}
