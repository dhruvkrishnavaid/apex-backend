package com.apex.repository;

import com.apex.model.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

  Optional<ProductVariant> findBySku(String sku);

  boolean existsBySku(String sku);

  List<ProductVariant> findByProductIdAndIsActiveTrue(UUID productId);

  Page<ProductVariant> findByProductId(UUID productId, Pageable pageable);
}
