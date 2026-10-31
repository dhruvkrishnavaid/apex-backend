package com.apex.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.SupplierRequests.CreateSupplierRequest;
import com.apex.dto.request.SupplierRequests.SupplierProductRequest;
import com.apex.dto.request.SupplierRequests.UpdateSupplierRequest;
import com.apex.dto.response.SupplierResponses.SupplierProductResponse;
import com.apex.dto.response.SupplierResponses.SupplierResponse;
import com.apex.exception.DuplicateResourceException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.model.ProductVariant;
import com.apex.model.Supplier;
import com.apex.model.SupplierProduct;
import com.apex.repository.ProductVariantRepository;
import com.apex.repository.SupplierProductRepository;
import com.apex.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplierService {

  private final SupplierRepository supplierRepository;
  private final SupplierProductRepository supplierProductRepository;
  private final ProductVariantRepository variantRepository;

  @Transactional
  public SupplierResponse create(CreateSupplierRequest r) {
    String code = r.code().trim().toUpperCase(Locale.ROOT);
    if (supplierRepository.existsByCode(code)) {
      throw new DuplicateResourceException("Supplier code already exists: " + code);
    }
    Supplier supplier = Supplier.builder()
        .code(code).name(r.name()).contactName(r.contactName()).email(r.email()).phone(r.phone())
        .addressLine(r.addressLine()).city(r.city()).country(r.country()).paymentTerms(r.paymentTerms())
        .leadTimeDays(r.leadTimeDays()).notes(r.notes())
        .build();
    return toResponse(supplierRepository.save(supplier));
  }

  @Transactional(readOnly = true)
  public Page<SupplierResponse> list(String keyword, Boolean active, Pageable pageable) {
    Specification<Supplier> spec = (root, query, cb) -> cb.conjunction();
    if (active != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("isActive"), active));
    }
    if (keyword != null && !keyword.isBlank()) {
      String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
      spec = spec.and((root, query, cb) -> cb.or(
          cb.like(cb.lower(root.get("name")), pattern),
          cb.like(cb.lower(root.get("code")), pattern)));
    }
    return supplierRepository.findAll(spec, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public SupplierResponse get(UUID id) {
    return toResponse(find(id));
  }

  @Transactional
  public SupplierResponse update(UUID id, UpdateSupplierRequest r) {
    Supplier s = find(id);
    s.setName(r.name());
    s.setContactName(r.contactName());
    s.setEmail(r.email());
    s.setPhone(r.phone());
    s.setAddressLine(r.addressLine());
    s.setCity(r.city());
    s.setCountry(r.country());
    s.setPaymentTerms(r.paymentTerms());
    s.setLeadTimeDays(r.leadTimeDays());
    s.setNotes(r.notes());
    if (r.isActive() != null) {
      s.setIsActive(r.isActive());
    }
    return toResponse(supplierRepository.save(s));
  }

  @Transactional
  public void deactivate(UUID id) {
    Supplier s = find(id);
    s.setIsActive(false);
    supplierRepository.save(s);
  }

  // ---------- Supplier catalogue ----------

  @Transactional
  public SupplierProductResponse addProduct(UUID supplierId, SupplierProductRequest r) {
    Supplier supplier = find(supplierId);
    ProductVariant variant = variantRepository.findById(r.variantId())
        .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + r.variantId()));
    if (supplierProductRepository.existsBySupplierIdAndVariantId(supplierId, r.variantId())) {
      throw new DuplicateResourceException("Supplier already supplies SKU " + variant.getSku());
    }
    SupplierProduct sp = SupplierProduct.builder()
        .supplier(supplier).variant(variant).supplierSku(r.supplierSku()).unitCost(r.unitCost())
        .minOrderQuantity(r.minOrderQuantity() != null ? r.minOrderQuantity() : 1)
        .isPreferred(Boolean.TRUE.equals(r.isPreferred()))
        .build();
    return toResponse(supplierProductRepository.save(sp));
  }

  @Transactional(readOnly = true)
  public List<SupplierProductResponse> listProducts(UUID supplierId) {
    find(supplierId);
    return supplierProductRepository.findBySupplier(supplierId).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<SupplierProductResponse> listSuppliersForVariant(UUID variantId) {
    return supplierProductRepository.findActiveByVariant(variantId).stream().map(this::toResponse).toList();
  }

  @Transactional
  public SupplierProductResponse updateProduct(UUID supplierId, UUID variantId, SupplierProductRequest r) {
    SupplierProduct sp = supplierProductRepository.findBySupplierIdAndVariantId(supplierId, variantId)
        .orElseThrow(() -> new ResourceNotFoundException("Supplier does not supply variant " + variantId));
    sp.setSupplierSku(r.supplierSku());
    sp.setUnitCost(r.unitCost());
    if (r.minOrderQuantity() != null) {
      sp.setMinOrderQuantity(r.minOrderQuantity());
    }
    if (r.isPreferred() != null) {
      sp.setIsPreferred(r.isPreferred());
    }
    return toResponse(supplierProductRepository.save(sp));
  }

  @Transactional
  public void removeProduct(UUID supplierId, UUID variantId) {
    SupplierProduct sp = supplierProductRepository.findBySupplierIdAndVariantId(supplierId, variantId)
        .orElseThrow(() -> new ResourceNotFoundException("Supplier does not supply variant " + variantId));
    supplierProductRepository.delete(sp);
  }

  private Supplier find(UUID id) {
    return supplierRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));
  }

  private SupplierResponse toResponse(Supplier s) {
    return new SupplierResponse(s.getId(), s.getCode(), s.getName(), s.getContactName(), s.getEmail(),
        s.getPhone(), s.getAddressLine(), s.getCity(), s.getCountry(), s.getPaymentTerms(),
        s.getLeadTimeDays(), s.getNotes(), s.getIsActive(), s.getCreatedAt());
  }

  private SupplierProductResponse toResponse(SupplierProduct sp) {
    return new SupplierProductResponse(sp.getId(), sp.getSupplier().getId(), sp.getSupplier().getName(),
        sp.getVariant().getId(), sp.getVariant().getSku(), sp.getVariant().getProduct().getName(),
        sp.getSupplierSku(), sp.getUnitCost(), sp.getMinOrderQuantity(), sp.getIsPreferred());
  }
}
