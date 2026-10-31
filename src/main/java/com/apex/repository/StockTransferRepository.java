package com.apex.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.apex.model.StockTransfer;

public interface StockTransferRepository extends JpaRepository<StockTransfer, UUID>, JpaSpecificationExecutor<StockTransfer> {
}
