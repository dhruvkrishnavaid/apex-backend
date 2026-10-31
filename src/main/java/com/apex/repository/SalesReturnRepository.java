package com.apex.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apex.model.SalesReturn;

public interface SalesReturnRepository extends JpaRepository<SalesReturn, UUID> {

  List<SalesReturn> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
