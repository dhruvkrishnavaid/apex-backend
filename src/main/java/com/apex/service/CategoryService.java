package com.apex.service;

import com.apex.dto.request.CategoryRequests.CreateCategoryRequest;
import com.apex.dto.request.CategoryRequests.UpdateCategoryRequest;
import com.apex.dto.response.CategoryResponse;
import com.apex.exception.ResourceNotFoundException;
import com.apex.mapper.CategoryMapper;
import com.apex.model.Category;
import com.apex.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

  private final CategoryRepository categoryRepository;
  private final CategoryMapper categoryMapper;

  @Transactional
  public CategoryResponse createCategory(CreateCategoryRequest request) {
    String slug = generateUniqueSlug(request.name());

    Category parent = null;
    if (request.parentId() != null) {
      parent = categoryRepository.findById(request.parentId())
          .orElseThrow(
              () -> new ResourceNotFoundException("Parent category not found: " + request.parentId()));
    }

    Category category = Category.builder()
        .name(request.name())
        .slug(slug)
        .description(request.description())
        .parent(parent)
        .isActive(true)
        .build();

    return categoryMapper.toResponse(categoryRepository.save(category));
  }

  @Transactional
  public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request) {
    Category category = categoryRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

    category.setName(request.name());
    if (request.description() != null) {
      category.setDescription(request.description());
    }
    if (request.isActive() != null) {
      category.setIsActive(request.isActive());
    }

    return categoryMapper.toResponse(categoryRepository.save(category));
  }

  @Transactional(readOnly = true)
  public CategoryResponse getCategory(UUID id) {
    Category category = categoryRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    return categoryMapper.toResponse(category);
  }

  @Transactional(readOnly = true)
  public CategoryResponse getCategoryBySlug(String slug) {
    Category category = categoryRepository.findBySlug(slug)
        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + slug));
    return categoryMapper.toResponse(category);
  }

  @Transactional(readOnly = true)
  public List<CategoryResponse> getCategoryTree() {
    return categoryRepository.findAllRootCategories().stream()
        .map(categoryMapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CategoryResponse> getSubcategories(UUID parentId) {
    return categoryRepository.findByParentId(parentId).stream()
        .map(categoryMapper::toResponse)
        .toList();
  }

  @Transactional
  public void deleteCategory(UUID id) {
    Category category = categoryRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

    if (category.getChildren() != null && !category.getChildren().isEmpty()) {
      throw new IllegalStateException(
          "Cannot delete category with active subcategories. Delete or reassign subcategories first.");
    }
    categoryRepository.delete(category);
  }

  private String generateUniqueSlug(String name) {
    String base = slugify(name);
    String slug = base;
    int counter = 1;
    while (categoryRepository.existsBySlug(slug)) {
      slug = base + "-" + counter++;
    }
    return slug;
  }

  private String slugify(String input) {
    if (input == null)
      return "";
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    return normalized.toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9\\s-]", "")
        .trim()
        .replaceAll("[\\s-]+", "-");
  }
}
