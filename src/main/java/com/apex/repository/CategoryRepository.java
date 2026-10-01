package com.apex.repository;

import com.apex.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

  Optional<Category> findBySlug(String slug);

  boolean existsBySlug(String slug);

  List<Category> findByParentId(UUID parentId);

  @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.isActive = true")
  List<Category> findAllRootCategories();
}
