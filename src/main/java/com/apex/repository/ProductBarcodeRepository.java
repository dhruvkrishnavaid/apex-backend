package com.apex.repository;

import com.apex.model.ProductBarcode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductBarcodeRepository extends JpaRepository<ProductBarcode, UUID> {

  Optional<ProductBarcode> findByBarcode(String barcode);

  boolean existsByBarcode(String barcode);

  List<ProductBarcode> findByVariantId(UUID variantId);

  @Query("SELECT b FROM ProductBarcode b JOIN FETCH b.variant v JOIN FETCH v.product WHERE b.barcode = :barcode")
  Optional<ProductBarcode> findBarcodeWithProductDetails(@Param("barcode") String barcode);
}
