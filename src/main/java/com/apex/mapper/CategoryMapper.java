package com.apex.mapper;

import com.apex.dto.response.CategoryResponse;
import com.apex.model.Category;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class CategoryMapper {

  public CategoryResponse toResponse(Category category) {
    if (category == null)
      return null;

    List<CategoryResponse> subcategories = category.getChildren() != null
        ? category.getChildren().stream().map(this::toResponse).toList()
        : Collections.emptyList();

    return new CategoryResponse(
        category.getId(),
        category.getParent() != null ? category.getParent().getId() : null,
        category.getName(),
        category.getSlug(),
        category.getDescription(),
        category.getIsActive(),
        subcategories,
        category.getCreatedAt());
  }
}
