package com.apex.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.apex.model.Warehouse;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {

  boolean existsByCode(String code);

  long countByIsActiveTrue();

  Optional<Warehouse> findByCode(String code);

  Page<Warehouse> findByIsActive(Boolean isActive, Pageable pageable);
}
