package com.apex.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.apex.model.SupplierProduct;

public interface SupplierProductRepository extends JpaRepository<SupplierProduct, UUID> {

  boolean existsBySupplierIdAndVariantId(UUID supplierId, UUID variantId);

  Optional<SupplierProduct> findBySupplierIdAndVariantId(UUID supplierId, UUID variantId);

  @Query("SELECT sp FROM SupplierProduct sp JOIN FETCH sp.variant v JOIN FETCH v.product WHERE sp.supplier.id = :supplierId ORDER BY v.sku")
  List<SupplierProduct> findBySupplier(@Param("supplierId") UUID supplierId);

  @Query("SELECT sp FROM SupplierProduct sp JOIN FETCH sp.supplier s WHERE sp.variant.id = :variantId AND s.isActive = true ORDER BY sp.isPreferred DESC, sp.unitCost ASC")
  List<SupplierProduct> findActiveByVariant(@Param("variantId") UUID variantId);
}
