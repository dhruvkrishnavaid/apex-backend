package com.apex.repository;

import com.apex.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

  boolean existsBySlug(String slug);

  /** Includes soft-deleted products, which still reference the category through a foreign key. */
  boolean existsByCategoryId(UUID categoryId);

  Optional<Product> findBySlugAndDeletedAtIsNull(String slug);

  boolean existsBySlugAndDeletedAtIsNull(String slug);

  Page<Product> findByCategoryIdAndDeletedAtIsNull(UUID categoryId, Pageable pageable);

}
