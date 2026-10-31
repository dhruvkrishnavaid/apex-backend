package com.apex.service;

import com.apex.dto.request.ProductRequests.CreateBarcodeRequest;
import com.apex.dto.request.ProductRequests.CreateProductRequest;
import com.apex.dto.request.ProductRequests.CreateMediaRequest;
import com.apex.dto.request.ProductRequests.CreateVariantRequest;
import com.apex.dto.request.ProductRequests.UpdateProductRequest;
import com.apex.dto.request.ProductRequests.UpdateVariantRequest;
import com.apex.dto.response.ProductResponses.BarcodeResponse;
import com.apex.dto.response.ProductResponses.MediaResponse;
import com.apex.dto.response.ProductResponses.ProductResponse;
import com.apex.dto.response.ProductResponses.VariantResponse;
import com.apex.exception.DuplicateResourceException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.mapper.ProductMapper;
import com.apex.model.Category;
import com.apex.model.MediaAsset;
import com.apex.model.Product;
import com.apex.model.ProductBarcode;
import com.apex.model.ProductBarcode.BarcodeType;
import com.apex.model.ProductVariant;
import com.apex.repository.CategoryRepository;
import com.apex.repository.MediaAssetRepository;
import com.apex.repository.ProductBarcodeRepository;
import com.apex.repository.ProductRepository;
import com.apex.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;
  private final ProductVariantRepository variantRepository;
  private final ProductBarcodeRepository barcodeRepository;
  private final CategoryRepository categoryRepository;
  private final MediaAssetRepository mediaAssetRepository;
  private final ProductMapper productMapper;

  // ---------- Products ----------

  @Transactional
  public ProductResponse createProduct(CreateProductRequest request) {
    Category category = categoryRepository.findById(request.categoryId())
        .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId()));

    Product product = Product.builder()
        .category(category)
        .name(request.name())
        .slug(generateUniqueSlug(request.name()))
        .description(request.description() != null ? request.description() : "")
        .brand(request.brand())
        .unitOfMeasure(request.unitOfMeasure())
        .hasVariants(Boolean.TRUE.equals(request.hasVariants()))
        .isActive(true)
        .build();

    product = productRepository.save(product);

    if (request.variants() != null && !request.variants().isEmpty()) {
      final Product savedProduct = product;
      List<ProductVariant> variants = request.variants().stream()
          .map(variantRequest -> buildVariant(savedProduct, variantRequest))
          .collect(Collectors.toCollection(ArrayList::new));
      product.setVariants(variants);
      product = productRepository.save(product);
    }

    return toFullResponse(product);
  }

  @Transactional(readOnly = true)
  public ProductResponse getProduct(UUID id) {
    Product product = findActiveProduct(id);
    return toFullResponse(product);
  }

  @Transactional(readOnly = true)
  public ProductResponse getProductBySlug(String slug) {
    Product product = productRepository.findBySlugAndDeletedAtIsNull(slug)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + slug));
    return toFullResponse(product);
  }

  @Transactional(readOnly = true)
  public Page<ProductResponse> listProducts(UUID categoryId, String keyword, Pageable pageable) {
    Specification<Product> spec = (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    if (categoryId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
    }
    if (keyword != null && !keyword.isBlank()) {
      String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
      spec = spec.and((root, query, cb) -> cb.or(
          cb.like(cb.lower(root.get("name")), pattern),
          cb.like(cb.lower(root.get("brand")), pattern)));
    }
    return productRepository.findAll(spec, pageable).map(this::toFullResponse);
  }

  @Transactional
  public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
    Product product = findActiveProduct(id);
    if (!product.getCategory().getId().equals(request.categoryId())) {
      product.setCategory(categoryRepository.findById(request.categoryId())
          .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.categoryId())));
    }
    product.setName(request.name());
    product.setDescription(request.description() != null ? request.description() : product.getDescription());
    product.setBrand(request.brand());
    product.setUnitOfMeasure(request.unitOfMeasure());
    if (request.isActive() != null) {
      product.setIsActive(request.isActive());
    }
    return toFullResponse(productRepository.save(product));
  }

  @Transactional
  public void deleteProduct(UUID id) {
    Product product = findActiveProduct(id);
    product.setDeletedAt(LocalDateTime.now());
    productRepository.save(product);
  }

  // ---------- Variants ----------

  @Transactional
  public VariantResponse addVariant(UUID productId, CreateVariantRequest request) {
    Product product = findActiveProduct(productId);
    ProductVariant variant = variantRepository.save(buildVariant(product, request));
    return productMapper.toVariantResponse(variant);
  }

  @Transactional(readOnly = true)
  public Page<VariantResponse> listVariants(UUID productId, Pageable pageable) {
    findActiveProduct(productId);
    return variantRepository.findByProductId(productId, pageable)
        .map(productMapper::toVariantResponse);
  }

  @Transactional(readOnly = true)
  public VariantResponse getVariantBySku(String sku) {
    ProductVariant variant = variantRepository.findBySku(sku)
        .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + sku));
    return productMapper.toVariantResponse(variant);
  }

  @Transactional
  public VariantResponse updateVariant(UUID variantId, UpdateVariantRequest request) {
    ProductVariant variant = findVariant(variantId);
    variant.setVariantAttributes(request.variantAttributes());
    variant.setCostPrice(request.costPrice());
    variant.setRetailPrice(request.retailPrice());
    variant.setDimensions(request.dimensions());
    variant.setWeight(request.weight());
    if (request.isActive() != null) {
      variant.setIsActive(request.isActive());
    }
    return productMapper.toVariantResponse(variantRepository.save(variant));
  }

  /** Variants are deactivated rather than deleted, since stock and order history reference them. */
  @Transactional
  public void deactivateVariant(UUID variantId) {
    ProductVariant variant = findVariant(variantId);
    variant.setIsActive(false);
    variantRepository.save(variant);
  }

  // ---------- Media ----------

  @Transactional
  public MediaResponse addMedia(UUID productId, CreateMediaRequest request) {
    Product product = findActiveProduct(productId);
    ProductVariant variant = null;
    if (request.variantId() != null) {
      variant = findVariant(request.variantId());
      if (!variant.getProduct().getId().equals(productId)) {
        throw new IllegalArgumentException("Variant does not belong to this product");
      }
    }
    boolean primary = Boolean.TRUE.equals(request.isPrimary());
    if (primary) {
      mediaAssetRepository.findByProductIdOrderBySortOrderAsc(productId)
          .forEach(existing -> existing.setIsPrimary(false));
    }
    MediaAsset asset = MediaAsset.builder()
        .product(product)
        .variant(variant)
        .url(request.url())
        .fileName(request.fileName())
        .mimeType(request.mimeType())
        .isPrimary(primary)
        .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
        .build();
    return productMapper.toMediaResponse(mediaAssetRepository.save(asset));
  }

  @Transactional(readOnly = true)
  public List<MediaResponse> listMedia(UUID productId) {
    findActiveProduct(productId);
    return mediaAssetRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
        .map(productMapper::toMediaResponse)
        .toList();
  }

  @Transactional
  public void deleteMedia(UUID mediaId) {
    MediaAsset asset = mediaAssetRepository.findById(mediaId)
        .orElseThrow(() -> new ResourceNotFoundException("Media asset not found: " + mediaId));
    mediaAssetRepository.delete(asset);
  }

  // ---------- Barcodes ----------

  @Transactional
  public BarcodeResponse addBarcode(UUID variantId, CreateBarcodeRequest request) {
    ProductVariant variant = variantRepository.findById(variantId)
        .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));

    validateBarcodeUniqueness(request.barcode());

    ProductBarcode barcode = buildBarcode(request);
    if (request.isPrimary() == null) {
      barcode.setIsPrimary(variant.getBarcodes().isEmpty());
    }
    if (Boolean.TRUE.equals(barcode.getIsPrimary())) {
      variant.getBarcodes().forEach(existing -> existing.setIsPrimary(false));
    }
    variant.addBarcode(barcode);
    barcode = barcodeRepository.save(barcode);
    return productMapper.toBarcodeResponse(barcode);
  }

  @Transactional
  public void removeBarcode(UUID barcodeId) {
    ProductBarcode barcode = barcodeRepository.findById(barcodeId)
        .orElseThrow(() -> new ResourceNotFoundException("Barcode not found: " + barcodeId));
    ProductVariant variant = barcode.getVariant();
    variant.removeBarcode(barcode);
    if (Boolean.TRUE.equals(barcode.getIsPrimary()) && !variant.getBarcodes().isEmpty()) {
      variant.getBarcodes().get(0).setIsPrimary(true);
    }
    variantRepository.save(variant);
  }

  @Transactional(readOnly = true)
  public VariantResponse lookupByBarcode(String barcode) {
    ProductBarcode found = barcodeRepository.findBarcodeWithProductDetails(barcode)
        .orElseThrow(() -> new ResourceNotFoundException("Barcode not found: " + barcode));
    return productMapper.toVariantResponse(found.getVariant());
  }

  // ---------- Private Helpers ----------

  private ProductVariant findVariant(UUID id) {
    return variantRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + id));
  }

  private Product findActiveProduct(UUID id) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    if (product.getDeletedAt() != null) {
      throw new ResourceNotFoundException("Product not found: " + id);
    }
    return product;
  }

  private ProductVariant buildVariant(Product product, CreateVariantRequest request) {
    if (variantRepository.existsBySku(request.sku())) {
      throw new DuplicateResourceException("SKU already exists: " + request.sku());
    }

    ProductVariant variant = ProductVariant.builder()
        .product(product)
        .sku(request.sku())
        .variantAttributes(request.variantAttributes())
        .costPrice(request.costPrice())
        .retailPrice(request.retailPrice())
        .dimensions(request.dimensions())
        .weight(request.weight())
        .isActive(true)
        .build();

    if (request.barcodes() != null) {
      for (CreateBarcodeRequest barcodeRequest : request.barcodes()) {
        validateBarcodeUniqueness(barcodeRequest.barcode());
        variant.addBarcode(buildBarcode(barcodeRequest));
      }
    }
    return variant;
  }

  private ProductBarcode buildBarcode(CreateBarcodeRequest request) {
    BarcodeType type;
    try {
      type = BarcodeType.valueOf(request.type().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid barcode type: " + request.type()
          + ". Valid types: UPC_A, EAN_13, CODE_128, QR_CODE");
    }

    return ProductBarcode.builder()
        .barcode(request.barcode())
        .type(type)
        .isPrimary(request.isPrimary() != null ? request.isPrimary() : true)
        .build();
  }

  private void validateBarcodeUniqueness(String barcode) {
    if (barcodeRepository.existsByBarcode(barcode)) {
      throw new DuplicateResourceException("Barcode already exists: " + barcode);
    }
  }

  private ProductResponse toFullResponse(Product product) {
    var mediaAssets = mediaAssetRepository.findByProductIdOrderBySortOrderAsc(product.getId());
    return productMapper.toProductResponse(product, mediaAssets);
  }

  private String generateUniqueSlug(String name) {
    String base = slugify(name);
    String slug = base;
    int counter = 1;
    while (productRepository.existsBySlug(slug)) {
      slug = base + "-" + counter++;
    }
    return slug;
  }

  private String slugify(String input) {
    if (input == null)
      return "";
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    return normalized.toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9\\s-]", "")
        .trim()
        .replaceAll("[\\s-]+", "-");
  }
}
