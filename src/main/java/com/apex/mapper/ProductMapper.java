package com.apex.mapper;

import com.apex.dto.response.ProductResponses.BarcodeResponse;
import com.apex.dto.response.ProductResponses.ProductResponse;
import com.apex.dto.response.ProductResponses.VariantResponse;
import com.apex.model.MediaAsset;
import com.apex.model.Product;
import com.apex.model.ProductBarcode;
import com.apex.model.ProductVariant;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class ProductMapper {

  public ProductResponse toProductResponse(Product product, List<MediaAsset> mediaAssets) {
    if (product == null)
      return null;

    List<VariantResponse> variants = product.getVariants() != null
        ? product.getVariants().stream().map(this::toVariantResponse).toList()
        : Collections.emptyList();

    List<String> mediaUrls = mediaAssets != null
        ? mediaAssets.stream().map(asset -> asset.getUrl()).toList()
        : Collections.emptyList();

    return new ProductResponse(
        product.getId(),
        product.getCategory() != null ? product.getCategory().getId() : null,
        product.getCategory() != null ? product.getCategory().getName() : null,
        product.getName(),
        product.getSlug(),
        product.getDescription(),
        product.getBrand(),
        product.getUnitOfMeasure(),
        product.getHasVariants(),
        product.getIsActive(),
        variants,
        mediaUrls,
        product.getCreatedAt());
  }

  public VariantResponse toVariantResponse(ProductVariant variant) {
    if (variant == null)
      return null;

    List<BarcodeResponse> barcodes = variant.getBarcodes() != null
        ? variant.getBarcodes().stream().map(this::toBarcodeResponse).toList()
        : Collections.emptyList();

    return new VariantResponse(
        variant.getId(),
        variant.getProduct() != null ? variant.getProduct().getId() : null,
        variant.getSku(),
        variant.getVariantAttributes(),
        variant.getCostPrice(),
        variant.getRetailPrice(),
        variant.getDimensions(),
        variant.getIsActive(),
        barcodes,
        variant.getCreatedAt());
  }

  public BarcodeResponse toBarcodeResponse(ProductBarcode barcode) {
    if (barcode == null)
      return null;

    return new BarcodeResponse(
        barcode.getId(),
        barcode.getBarcode(),
        barcode.getType() != null ? barcode.getType().name() : null,
        barcode.getIsPrimary());
  }
}
