package com.apex.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.InventoryRequests.AdjustStockRequest;
import com.apex.dto.request.InventoryRequests.StockCountRequest;
import com.apex.dto.response.InventoryResponses.MovementResponse;
import com.apex.dto.response.InventoryResponses.StockLevelResponse;
import com.apex.dto.response.InventoryResponses.VariantStockSummary;
import com.apex.exception.BusinessRuleException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.model.ProductVariant;
import com.apex.model.StockLevel;
import com.apex.model.StockMovement;
import com.apex.model.StockMovement.MovementType;
import com.apex.model.StorageLocation;
import com.apex.model.Warehouse;
import com.apex.repository.ProductVariantRepository;
import com.apex.repository.StockLevelRepository;
import com.apex.repository.StockMovementRepository;
import com.apex.repository.StorageLocationRepository;
import com.apex.repository.WarehouseRepository;
import com.apex.security.CurrentUser;

import lombok.RequiredArgsConstructor;

/**
 * Single entry point for every change of stock. Other modules (transfers, purchase receipts, orders)
 * call the primitives below so that stock levels and the movement ledger can never drift apart.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

  private final StockLevelRepository stockLevelRepository;
  private final StockMovementRepository movementRepository;
  private final ProductVariantRepository variantRepository;
  private final WarehouseRepository warehouseRepository;
  private final StorageLocationRepository locationRepository;
  private final AlertService alertService;

  // ---------- Queries ----------

  @Transactional(readOnly = true)
  public Page<StockLevelResponse> listStock(UUID warehouseId, UUID variantId, boolean inStockOnly, Pageable pageable) {
    Specification<StockLevel> spec = (root, query, cb) -> cb.conjunction();
    if (warehouseId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("warehouse").get("id"), warehouseId));
    }
    if (variantId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("variant").get("id"), variantId));
    }
    if (inStockOnly) {
      spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("quantityOnHand"), 0));
    }
    return stockLevelRepository.findAll(spec, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public VariantStockSummary getVariantSummary(UUID variantId) {
    ProductVariant variant = findVariant(variantId);
    List<StockLevelResponse> levels = stockLevelRepository.findByVariant(variantId).stream()
        .map(this::toResponse).toList();
    long onHand = levels.stream().mapToLong(StockLevelResponse::quantityOnHand).sum();
    long reserved = levels.stream().mapToLong(StockLevelResponse::quantityReserved).sum();
    return new VariantStockSummary(variantId, variant.getSku(), variant.getProduct().getName(),
        onHand, reserved, onHand - reserved, levels);
  }

  @Transactional(readOnly = true)
  public Page<MovementResponse> listMovements(UUID variantId, UUID warehouseId, MovementType type,
      LocalDate from, LocalDate to, Pageable pageable) {
    Specification<StockMovement> spec = (root, query, cb) -> cb.conjunction();
    if (variantId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("variant").get("id"), variantId));
    }
    if (warehouseId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("warehouse").get("id"), warehouseId));
    }
    if (type != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
    }
    if (from != null) {
      spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
    }
    if (to != null) {
      spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
    }
    return movementRepository.findAll(spec, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public long availableQuantity(UUID variantId, UUID warehouseId) {
    return stockLevelRepository.findByVariantIdAndWarehouseId(variantId, warehouseId)
        .map(StockLevel::getAvailable).orElse(0);
  }

  // ---------- Manual operations ----------

  @Transactional
  public StockLevelResponse adjust(AdjustStockRequest request) {
    if (request.quantityChange() == 0) {
      throw new IllegalArgumentException("Quantity change must not be zero");
    }
    MovementType type = request.quantityChange() < 0 && Boolean.TRUE.equals(request.damaged())
        ? MovementType.DAMAGE
        : MovementType.ADJUSTMENT;
    StockLevel level = apply(request.variantId(), request.warehouseId(), request.quantityChange(), type,
        "MANUAL", null, request.reason());
    return toResponse(level);
  }

  @Transactional
  public StockLevelResponse stockCount(StockCountRequest request) {
    StockLevel level = lockOrCreate(request.variantId(), request.warehouseId());
    int delta = request.countedQuantity() - level.getQuantityOnHand();
    if (request.countedQuantity() < level.getQuantityReserved()) {
      throw new BusinessRuleException("Counted quantity (" + request.countedQuantity()
          + ") is lower than the quantity reserved for open orders (" + level.getQuantityReserved() + ")");
    }
    if (delta == 0) {
      return toResponse(level);
    }
    return toResponse(apply(request.variantId(), request.warehouseId(), delta, MovementType.STOCK_COUNT,
        "STOCK_COUNT", null, request.reason() != null ? request.reason() : "Physical stock count"));
  }

  @Transactional
  public StockLevelResponse assignLocation(UUID variantId, UUID warehouseId, UUID locationId) {
    StockLevel level = lockOrCreate(variantId, warehouseId);
    StorageLocation location = null;
    if (locationId != null) {
      location = locationRepository.findById(locationId)
          .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + locationId));
      if (!location.getWarehouse().getId().equals(warehouseId)) {
        throw new IllegalArgumentException("Location does not belong to this warehouse");
      }
    }
    level.setLocation(location);
    return toResponse(stockLevelRepository.save(level));
  }

  // ---------- Primitives used by other modules ----------

  /**
   * Changes quantity on hand by {@code delta} (positive or negative) and writes a ledger entry.
   * Outgoing quantity may only come from stock that is not reserved.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public StockLevel apply(UUID variantId, UUID warehouseId, int delta, MovementType type,
      String referenceType, UUID referenceId, String reason) {
    StockLevel level = lockOrCreate(variantId, warehouseId);
    if (delta > 0) {
      requireActiveWarehouse(level.getWarehouse());
    }
    int newOnHand = level.getQuantityOnHand() + delta;
    if (newOnHand < level.getQuantityReserved()) {
      throw new BusinessRuleException("Insufficient available stock for SKU " + level.getVariant().getSku()
          + " in warehouse " + level.getWarehouse().getCode() + ": available " + level.getAvailable()
          + ", requested " + (-delta));
    }
    level.setQuantityOnHand(newOnHand);
    record(level, type, delta, referenceType, referenceId, reason);
    level = stockLevelRepository.saveAndFlush(level);
    alertService.evaluate(variantId, warehouseId);
    return level;
  }

  /** Reserves stock for an order or outgoing transfer. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void reserve(UUID variantId, UUID warehouseId, int quantity) {
    requirePositive(quantity);
    StockLevel level = lockOrCreate(variantId, warehouseId);
    if (level.getAvailable() < quantity) {
      throw new BusinessRuleException("Insufficient available stock for SKU " + level.getVariant().getSku()
          + " in warehouse " + level.getWarehouse().getCode() + ": available " + level.getAvailable()
          + ", requested " + quantity);
    }
    level.setQuantityReserved(level.getQuantityReserved() + quantity);
    stockLevelRepository.saveAndFlush(level);
    alertService.evaluate(variantId, warehouseId);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void release(UUID variantId, UUID warehouseId, int quantity) {
    requirePositive(quantity);
    StockLevel level = lockOrCreate(variantId, warehouseId);
    if (level.getQuantityReserved() < quantity) {
      throw new IllegalStateException("Cannot release " + quantity + " units: only "
          + level.getQuantityReserved() + " reserved");
    }
    level.setQuantityReserved(level.getQuantityReserved() - quantity);
    stockLevelRepository.saveAndFlush(level);
    alertService.evaluate(variantId, warehouseId);
  }

  /** Removes previously reserved stock from the warehouse (shipment / transfer dispatch). */
  @Transactional(propagation = Propagation.MANDATORY)
  public void consumeReserved(UUID variantId, UUID warehouseId, int quantity, MovementType type,
      String referenceType, UUID referenceId, String reason) {
    requirePositive(quantity);
    StockLevel level = lockOrCreate(variantId, warehouseId);
    if (level.getQuantityReserved() < quantity) {
      throw new IllegalStateException("Cannot consume " + quantity + " units: only "
          + level.getQuantityReserved() + " reserved");
    }
    level.setQuantityReserved(level.getQuantityReserved() - quantity);
    level.setQuantityOnHand(level.getQuantityOnHand() - quantity);
    record(level, type, -quantity, referenceType, referenceId, reason);
    stockLevelRepository.saveAndFlush(level);
    alertService.evaluate(variantId, warehouseId);
  }

  // ---------- Helpers ----------

  private StockLevel lockOrCreate(UUID variantId, UUID warehouseId) {
    return stockLevelRepository.findForUpdate(variantId, warehouseId).orElseGet(() -> {
      ProductVariant variant = findVariant(variantId);
      Warehouse warehouse = warehouseRepository.findById(warehouseId)
          .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + warehouseId));
      if (!Boolean.TRUE.equals(variant.getIsActive())) {
        throw new IllegalStateException("Variant is inactive: " + variant.getSku());
      }
      return stockLevelRepository.saveAndFlush(StockLevel.builder().variant(variant).warehouse(warehouse).build());
    });
  }

  private void record(StockLevel level, MovementType type, int delta, String referenceType,
      UUID referenceId, String reason) {
    movementRepository.save(StockMovement.builder()
        .variant(level.getVariant())
        .warehouse(level.getWarehouse())
        .type(type)
        .quantityChange(delta)
        .quantityAfter(level.getQuantityOnHand())
        .referenceType(referenceType)
        .referenceId(referenceId)
        .reason(reason)
        .performedBy(CurrentUser.id().orElse(null))
        .build());
  }

  private void requireActiveWarehouse(Warehouse warehouse) {
    if (!Boolean.TRUE.equals(warehouse.getIsActive())) {
      throw new IllegalStateException("Warehouse is inactive: " + warehouse.getCode());
    }
  }

  private void requirePositive(int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be greater than zero");
    }
  }

  private ProductVariant findVariant(UUID id) {
    return variantRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + id));
  }

  private StockLevelResponse toResponse(StockLevel s) {
    return new StockLevelResponse(s.getId(), s.getVariant().getId(), s.getVariant().getSku(),
        s.getVariant().getProduct().getName(), s.getWarehouse().getId(), s.getWarehouse().getCode(),
        s.getLocation() != null ? s.getLocation().getId() : null,
        s.getLocation() != null ? s.getLocation().getCode() : null,
        s.getQuantityOnHand(), s.getQuantityReserved(), s.getAvailable(), s.getUpdatedAt());
  }

  private MovementResponse toResponse(StockMovement m) {
    return new MovementResponse(m.getId(), m.getVariant().getId(), m.getVariant().getSku(),
        m.getWarehouse().getId(), m.getWarehouse().getCode(), m.getType().name(), m.getQuantityChange(),
        m.getQuantityAfter(), m.getReferenceType(), m.getReferenceId(), m.getReason(), m.getPerformedBy(),
        m.getCreatedAt());
  }
}
