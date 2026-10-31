package com.apex.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apex.model.ReorderRule;

public interface ReorderRuleRepository extends JpaRepository<ReorderRule, UUID> {

  Optional<ReorderRule> findByVariantIdAndWarehouseId(UUID variantId, UUID warehouseId);

  List<ReorderRule> findByIsActiveTrue();

  List<ReorderRule> findByWarehouseId(UUID warehouseId);
}
