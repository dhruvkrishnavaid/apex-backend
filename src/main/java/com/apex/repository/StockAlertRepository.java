package com.apex.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.apex.model.StockAlert;
import com.apex.model.StockAlert.Status;

public interface StockAlertRepository extends JpaRepository<StockAlert, UUID> {

  List<StockAlert> findByVariantIdAndWarehouseIdAndStatusIn(UUID variantId, UUID warehouseId, Collection<Status> statuses);

  Page<StockAlert> findByStatusIn(Collection<Status> statuses, Pageable pageable);

  Page<StockAlert> findByStatusInAndWarehouseId(Collection<Status> statuses, UUID warehouseId, Pageable pageable);

  long countByStatus(Status status);
}
