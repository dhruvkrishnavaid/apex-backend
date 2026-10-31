package com.apex.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apex.model.StorageLocation;

public interface StorageLocationRepository extends JpaRepository<StorageLocation, UUID> {

  boolean existsByWarehouseIdAndCode(UUID warehouseId, String code);

  List<StorageLocation> findByWarehouseIdOrderByCodeAsc(UUID warehouseId);
}
