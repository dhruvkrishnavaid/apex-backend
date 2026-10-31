package com.apex.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apex.model.GoodsReceipt;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, UUID> {

  List<GoodsReceipt> findByPurchaseOrderIdOrderByReceivedAtAsc(UUID purchaseOrderId);
}
