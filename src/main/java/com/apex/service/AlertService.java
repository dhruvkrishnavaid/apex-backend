package com.apex.service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.AlertRequests.ReorderRuleRequest;
import com.apex.dto.response.AlertResponses.AlertResponse;
import com.apex.dto.response.AlertResponses.AlertSummary;
import com.apex.dto.response.AlertResponses.ReorderRuleResponse;
import com.apex.dto.response.AlertResponses.ReplenishmentSuggestion;
import com.apex.exception.DuplicateResourceException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.model.ProductVariant;
import com.apex.model.ReorderRule;
import com.apex.model.StockAlert;
import com.apex.model.StockAlert.Status;
import com.apex.model.StockAlert.Type;
import com.apex.model.StockLevel;
import com.apex.model.Warehouse;
import com.apex.repository.ProductVariantRepository;
import com.apex.repository.ReorderRuleRepository;
import com.apex.repository.StockAlertRepository;
import com.apex.repository.StockLevelRepository;
import com.apex.repository.SupplierProductRepository;
import com.apex.repository.WarehouseRepository;
import com.apex.security.CurrentUser;

import lombok.RequiredArgsConstructor;

/**
 * Reorder rules and low-stock alerts. {@link #evaluate} is called by the inventory service after every
 * change of stock, inside the same transaction, so alerts always reflect committed quantities.
 */
@Service
@RequiredArgsConstructor
public class AlertService {

  private static final Logger log = LoggerFactory.getLogger(AlertService.class);
  private static final EnumSet<Status> ACTIVE = EnumSet.of(Status.OPEN, Status.ACKNOWLEDGED);

  private final ReorderRuleRepository ruleRepository;
  private final StockAlertRepository alertRepository;
  private final StockLevelRepository stockLevelRepository;
  private final ProductVariantRepository variantRepository;
  private final WarehouseRepository warehouseRepository;
  private final SupplierProductRepository supplierProductRepository;

  // ---------- Evaluation ----------

  @Transactional
  public void evaluate(UUID variantId, UUID warehouseId) {
    ReorderRule rule = ruleRepository.findByVariantIdAndWarehouseId(variantId, warehouseId).orElse(null);
    List<StockAlert> active = alertRepository.findByVariantIdAndWarehouseIdAndStatusIn(variantId, warehouseId, ACTIVE);

    if (rule == null || !Boolean.TRUE.equals(rule.getIsActive())) {
      resolve(active);
      return;
    }
    int available = stockLevelRepository.findByVariantIdAndWarehouseId(variantId, warehouseId)
        .map(StockLevel::getAvailable).orElse(0);

    if (available > rule.getReorderPoint()) {
      resolve(active);
      return;
    }
    Type type = available <= 0 ? Type.OUT_OF_STOCK : Type.LOW_STOCK;
    if (active.isEmpty()) {
      alertRepository.save(StockAlert.builder()
          .variant(rule.getVariant()).warehouse(rule.getWarehouse()).type(type)
          .availableQuantity(available).reorderPoint(rule.getReorderPoint()).build());
      log.warn("Stock alert {}: SKU {} in {} has {} available (reorder point {})", type,
          rule.getVariant().getSku(), rule.getWarehouse().getCode(), available, rule.getReorderPoint());
    } else {
      active.forEach(a -> {
        a.setType(type);
        a.setAvailableQuantity(available);
        a.setReorderPoint(rule.getReorderPoint());
      });
      alertRepository.saveAll(active);
    }
  }

  private void resolve(List<StockAlert> active) {
    if (active.isEmpty()) {
      return;
    }
    LocalDateTime now = LocalDateTime.now();
    active.forEach(a -> {
      a.setStatus(Status.RESOLVED);
      a.setResolvedAt(now);
    });
    alertRepository.saveAll(active);
  }

  // ---------- Rules ----------

  @Transactional
  public ReorderRuleResponse createRule(ReorderRuleRequest r) {
    if (ruleRepository.findByVariantIdAndWarehouseId(r.variantId(), r.warehouseId()).isPresent()) {
      throw new DuplicateResourceException("A reorder rule already exists for this SKU and warehouse");
    }
    ProductVariant variant = variantRepository.findById(r.variantId())
        .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + r.variantId()));
    Warehouse warehouse = warehouseRepository.findById(r.warehouseId())
        .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + r.warehouseId()));
    ReorderRule rule = ruleRepository.save(ReorderRule.builder()
        .variant(variant).warehouse(warehouse).reorderPoint(r.reorderPoint())
        .reorderQuantity(r.reorderQuantity()).isActive(r.isActive() == null || r.isActive()).build());
    ruleRepository.flush();
    evaluate(variant.getId(), warehouse.getId());
    return toResponse(rule);
  }

  @Transactional
  public ReorderRuleResponse updateRule(UUID id, ReorderRuleRequest r) {
    ReorderRule rule = findRule(id);
    rule.setReorderPoint(r.reorderPoint());
    rule.setReorderQuantity(r.reorderQuantity());
    if (r.isActive() != null) {
      rule.setIsActive(r.isActive());
    }
    ruleRepository.saveAndFlush(rule);
    evaluate(rule.getVariant().getId(), rule.getWarehouse().getId());
    return toResponse(rule);
  }

  @Transactional
  public void deleteRule(UUID id) {
    ReorderRule rule = findRule(id);
    ruleRepository.delete(rule);
    ruleRepository.flush();
    evaluate(rule.getVariant().getId(), rule.getWarehouse().getId());
  }

  @Transactional(readOnly = true)
  public List<ReorderRuleResponse> listRules(UUID warehouseId) {
    List<ReorderRule> rules = warehouseId == null ? ruleRepository.findAll() : ruleRepository.findByWarehouseId(warehouseId);
    return rules.stream().map(this::toResponse).toList();
  }

  // ---------- Alerts ----------

  @Transactional(readOnly = true)
  public Page<AlertResponse> listAlerts(Status status, UUID warehouseId, Pageable pageable) {
    var statuses = status == null ? ACTIVE : EnumSet.of(status);
    Page<StockAlert> page = warehouseId == null
        ? alertRepository.findByStatusIn(statuses, pageable)
        : alertRepository.findByStatusInAndWarehouseId(statuses, warehouseId, pageable);
    return page.map(this::toResponse);
  }

  @Transactional
  public AlertResponse acknowledge(UUID id) {
    StockAlert alert = alertRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + id));
    if (alert.getStatus() == Status.RESOLVED) {
      throw new IllegalStateException("Alert is already resolved");
    }
    alert.setStatus(Status.ACKNOWLEDGED);
    alert.setAcknowledgedAt(LocalDateTime.now());
    alert.setAcknowledgedBy(CurrentUser.id().orElse(null));
    return toResponse(alertRepository.save(alert));
  }

  @Transactional(readOnly = true)
  public AlertSummary summary() {
    return new AlertSummary(alertRepository.countByStatus(Status.OPEN),
        alertRepository.countByStatus(Status.ACKNOWLEDGED), alertRepository.countByStatus(Status.RESOLVED));
  }

  /** Items at or below their reorder point, with the quantity to order and the cheapest preferred supplier. */
  @Transactional(readOnly = true)
  public List<ReplenishmentSuggestion> suggestions(UUID warehouseId) {
    return ruleRepository.findByIsActiveTrue().stream()
        .filter(r -> warehouseId == null || r.getWarehouse().getId().equals(warehouseId))
        .map(r -> {
          int available = stockLevelRepository
              .findByVariantIdAndWarehouseId(r.getVariant().getId(), r.getWarehouse().getId())
              .map(StockLevel::getAvailable).orElse(0);
          return new Object[] { r, available };
        })
        .filter(pair -> (int) pair[1] <= ((ReorderRule) pair[0]).getReorderPoint())
        .map(pair -> {
          ReorderRule r = (ReorderRule) pair[0];
          var supplier = supplierProductRepository.findActiveByVariant(r.getVariant().getId()).stream().findFirst();
          return new ReplenishmentSuggestion(r.getVariant().getId(), r.getVariant().getSku(),
              r.getVariant().getProduct().getName(), r.getWarehouse().getId(), r.getWarehouse().getCode(),
              (int) pair[1], r.getReorderPoint(), r.getReorderQuantity(),
              supplier.map(s -> s.getSupplier().getId()).orElse(null),
              supplier.map(s -> s.getSupplier().getName()).orElse(null),
              supplier.map(s -> s.getUnitCost()).orElse(r.getVariant().getCostPrice()));
        })
        .toList();
  }

  private ReorderRule findRule(UUID id) {
    return ruleRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Reorder rule not found: " + id));
  }

  private ReorderRuleResponse toResponse(ReorderRule r) {
    return new ReorderRuleResponse(r.getId(), r.getVariant().getId(), r.getVariant().getSku(),
        r.getVariant().getProduct().getName(), r.getWarehouse().getId(), r.getWarehouse().getCode(),
        r.getReorderPoint(), r.getReorderQuantity(), r.getIsActive());
  }

  private AlertResponse toResponse(StockAlert a) {
    return new AlertResponse(a.getId(), a.getVariant().getId(), a.getVariant().getSku(),
        a.getVariant().getProduct().getName(), a.getWarehouse().getId(), a.getWarehouse().getCode(),
        a.getType().name(), a.getStatus().name(), a.getAvailableQuantity(), a.getReorderPoint(),
        a.getCreatedAt(), a.getAcknowledgedAt(), a.getResolvedAt());
  }
}
