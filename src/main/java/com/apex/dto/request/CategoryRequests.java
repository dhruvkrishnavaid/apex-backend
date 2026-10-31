package com.apex.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class CategoryRequests {

  public record CreateCategoryRequest(
      @NotBlank(message = "Category name is required") String name,
      String description,
      UUID parentId) {
  }

  /** A null parentId moves the category to the root level. */
  public record MoveCategoryRequest(UUID parentId) {
  }

  public record UpdateCategoryRequest(
      @NotBlank(message = "Category name is required") String name,
      String description,
      Boolean isActive) {
  }
}
