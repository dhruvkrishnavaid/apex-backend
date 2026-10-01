package com.apex.repository;

import com.apex.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

  Optional<Product> findBySlugAndDeletedAtIsNull(String slug);

  boolean existsBySlugAndDeletedAtIsNull(String slug);

  Page<Product> findByCategoryIdAndDeletedAtIsNull(UUID categoryId, Pageable pageable);

  @Query("SELECT p FROM Product p WHERE p.deletedAt IS NULL AND " +
      "(LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
      "LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%')))")
  Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);
}
