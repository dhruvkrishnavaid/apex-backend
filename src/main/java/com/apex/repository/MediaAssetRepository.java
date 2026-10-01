package com.apex.repository;

import com.apex.model.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

  List<MediaAsset> findByProductIdOrderBySortOrderAsc(UUID productId);

  List<MediaAsset> findByVariantIdOrderBySortOrderAsc(UUID variantId);
}
