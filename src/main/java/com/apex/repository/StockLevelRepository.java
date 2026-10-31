package com.apex.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.apex.model.StockLevel;

import jakarta.persistence.LockModeType;

public interface StockLevelRepository extends JpaRepository<StockLevel, UUID>, JpaSpecificationExecutor<StockLevel> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM StockLevel s WHERE s.variant.id = :variantId AND s.warehouse.id = :warehouseId")
  Optional<StockLevel> findForUpdate(@Param("variantId") UUID variantId, @Param("warehouseId") UUID warehouseId);

  Optional<StockLevel> findByVariantIdAndWarehouseId(UUID variantId, UUID warehouseId);

  @Query("SELECT s FROM StockLevel s JOIN FETCH s.warehouse WHERE s.variant.id = :variantId ORDER BY s.warehouse.code")
  List<StockLevel> findByVariant(@Param("variantId") UUID variantId);

  boolean existsByWarehouseIdAndQuantityOnHandGreaterThan(UUID warehouseId, int quantity);

  boolean existsByVariantIdAndQuantityOnHandGreaterThan(UUID variantId, int quantity);

  @Query("SELECT COALESCE(SUM(s.quantityOnHand), 0) FROM StockLevel s WHERE s.variant.id = :variantId")
  long totalOnHand(@Param("variantId") UUID variantId);

  @Query("SELECT COALESCE(SUM(s.quantityOnHand - s.quantityReserved), 0) FROM StockLevel s WHERE s.variant.id = :variantId")
  long totalAvailable(@Param("variantId") UUID variantId);

  Page<StockLevel> findByWarehouseId(UUID warehouseId, Pageable pageable);
}
