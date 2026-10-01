package com.apex.service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.ProductRequests.CreateBarcodeRequest;
import com.apex.dto.request.ProductRequests.CreateProductRequest;
import com.apex.dto.request.ProductRequests.CreateVariantRequest;
import com.apex.dto.response.ProductResponses.BarcodeResponse;
import com.apex.dto.response.ProductResponses.ProductResponse;
import com.apex.dto.response.ProductResponses.VariantResponse;
import com.apex.exception.DuplicateResourceException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.mapper.ProductMapper;
import com.apex.model.Category;
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
        .description(request.description())
        .brand(request.brand())
        .unitOfMeasure(request.unitOfMeasure())
        .hasVariants(Boolean.TRUE.equals(request.hasVariants()))
        .build();

    product = productRepository.save(product);

    if (request.variants() != null && !request.variants().isEmpty()) {
      final Product savedProduct = product;
      List<ProductVariant> variants = request.variants().stream()
          .map(variantRequest -> buildVariant(savedProduct, variantRequest))
          .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
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
    Page<Product> page;
    if (keyword != null && !keyword.isBlank()) {
      page = productRepository.searchProducts(keyword, pageable);
    } else if (categoryId != null) {
      page = productRepository.findByCategoryIdAndDeletedAtIsNull(categoryId, pageable);
    } else {
      page = productRepository.findAll(pageable);
    }
    return page.map(ProductService.this::toFullResponse);
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
    findActiveProduct(productId); // ensure product exists
    return variantRepository.findByProductId(productId, pageable)
        .map(productMapper::toVariantResponse);
  }

  @Transactional(readOnly = true)
  public VariantResponse getVariantBySku(String sku) {
    ProductVariant variant = variantRepository.findBySku(sku)
        .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + sku));
    return productMapper.toVariantResponse(variant);
  }

  // ---------- Barcodes ----------

  @Transactional
  public BarcodeResponse addBarcode(UUID variantId, CreateBarcodeRequest request) {
    ProductVariant variant = variantRepository.findById(variantId)
        .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));

    validateBarcodeUniqueness(request.barcode());

    ProductBarcode barcode = buildBarcode(request);
    variant.addBarcode(barcode);
    variantRepository.save(variant);
    return productMapper.toBarcodeResponse(barcode);
  }

  @Transactional(readOnly = true)
  public VariantResponse lookupByBarcode(String barcode) {
    ProductBarcode found = barcodeRepository.findBarcodeWithProductDetails(barcode)
        .orElseThrow(() -> new ResourceNotFoundException("Barcode not found: " + barcode));
    return productMapper.toVariantResponse(found.getVariant());
  }

  // ---------- Helpers ----------

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
        .isPrimary(request.isPrimary() == null || request.isPrimary())
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
    while (productRepository.existsBySlugAndDeletedAtIsNull(slug)) {
      slug = base + "-" + counter++;
    }
    return slug;
  }

  private String slugify(String input) {
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    return normalized.toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9\\s-]", "")
        .trim()
        .replaceAll("[\\s-]+", "-");
  }
}
