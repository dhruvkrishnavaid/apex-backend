package com.apex.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.PurchaseOrderRequests.CreatePurchaseOrderRequest;
import com.apex.dto.request.PurchaseOrderRequests.PoLineRequest;
import com.apex.dto.request.PurchaseOrderRequests.ReceiveLine;
import com.apex.dto.request.PurchaseOrderRequests.ReceivePurchaseOrderRequest;
import com.apex.dto.request.PurchaseOrderRequests.UpdatePurchaseOrderRequest;
import com.apex.dto.response.PurchaseOrderResponses.GoodsReceiptResponse;
import com.apex.dto.response.PurchaseOrderResponses.PoLineResponse;
import com.apex.dto.response.PurchaseOrderResponses.PurchaseOrderResponse;
import com.apex.dto.response.PurchaseOrderResponses.ReceiptLineResponse;
import com.apex.exception.BusinessRuleException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.model.GoodsReceipt;
import com.apex.model.GoodsReceiptLine;
import com.apex.model.ProductVariant;
import com.apex.model.PurchaseOrder;
import com.apex.model.PurchaseOrder.Status;
import com.apex.model.PurchaseOrderLine;
import com.apex.model.StockMovement.MovementType;
import com.apex.model.Supplier;
import com.apex.model.Warehouse;
import com.apex.repository.GoodsReceiptRepository;
import com.apex.repository.ProductVariantRepository;
import com.apex.repository.PurchaseOrderRepository;
import com.apex.repository.SupplierProductRepository;
import com.apex.repository.SupplierRepository;
import com.apex.repository.WarehouseRepository;
import com.apex.security.CurrentUser;

import lombok.RequiredArgsConstructor;

/**
 * Purchase order lifecycle: DRAFT -> ORDERED -> PARTIALLY_RECEIVED -> RECEIVED.
 * An order with no deliveries can be CANCELLED; a partially delivered one can be CLOSED.
 */
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

  private final PurchaseOrderRepository purchaseOrderRepository;
  private final GoodsReceiptRepository receiptRepository;
  private final SupplierRepository supplierRepository;
  private final SupplierProductRepository supplierProductRepository;
  private final WarehouseRepository warehouseRepository;
  private final ProductVariantRepository variantRepository;
  private final InventoryService inventoryService;

  @Transactional
  public PurchaseOrderResponse create(CreatePurchaseOrderRequest request) {
    Supplier supplier = supplierRepository.findById(request.supplierId())
        .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + request.supplierId()));
    if (!Boolean.TRUE.equals(supplier.getIsActive())) {
      throw new IllegalStateException("Supplier is inactive: " + supplier.getCode());
    }
    PurchaseOrder po = PurchaseOrder.builder()
        .poNumber(ReferenceNumbers.next("PO"))
        .supplier(supplier)
        .warehouse(activeWarehouse(request.warehouseId()))
        .expectedDate(request.expectedDate())
        .notes(request.notes())
        .createdBy(CurrentUser.id().orElse(null))
        .build();
    fillLines(po, request.lines());
    return toResponse(purchaseOrderRepository.save(po));
  }

  @Transactional
  public PurchaseOrderResponse update(UUID id, UpdatePurchaseOrderRequest request) {
    PurchaseOrder po = find(id);
    requireStatus(po, "edited", Status.DRAFT);
    po.setWarehouse(activeWarehouse(request.warehouseId()));
    po.setExpectedDate(request.expectedDate());
    po.setNotes(request.notes());
    po.getLines().clear();
    purchaseOrderRepository.flush();
    fillLines(po, request.lines());
    return toResponse(purchaseOrderRepository.save(po));
  }

  @Transactional
  public PurchaseOrderResponse submit(UUID id) {
    PurchaseOrder po = find(id);
    requireStatus(po, "submitted", Status.DRAFT);
    po.setStatus(Status.ORDERED);
    po.setOrderedAt(LocalDateTime.now());
    return toResponse(purchaseOrderRepository.save(po));
  }

  @Transactional
  public GoodsReceiptResponse receive(UUID id, ReceivePurchaseOrderRequest request) {
    PurchaseOrder po = find(id);
    requireStatus(po, "received against", Status.ORDERED, Status.PARTIALLY_RECEIVED);

    Map<UUID, Integer> quantities = new LinkedHashMap<>();
    for (ReceiveLine l : request.lines()) {
      quantities.merge(l.lineId(), l.quantity(), Integer::sum);
    }

    GoodsReceipt receipt = GoodsReceipt.builder()
        .receiptNumber(ReferenceNumbers.next("GRN"))
        .purchaseOrder(po)
        .notes(request.notes())
        .receivedBy(CurrentUser.id().orElse(null))
        .build();

    quantities.forEach((lineId, qty) -> {
      PurchaseOrderLine line = po.getLines().stream().filter(l -> l.getId().equals(lineId)).findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Line does not belong to this purchase order: " + lineId));
      if (qty > line.getOutstanding()) {
        throw new BusinessRuleException("Cannot receive " + qty + " of SKU " + line.getVariant().getSku()
            + ": only " + line.getOutstanding() + " outstanding");
      }
      line.setQuantityReceived(line.getQuantityReceived() + qty);
      inventoryService.apply(line.getVariant().getId(), po.getWarehouse().getId(), qty,
          MovementType.PURCHASE_RECEIPT, "PURCHASE_ORDER", po.getId(), po.getPoNumber());
      receipt.addLine(GoodsReceiptLine.builder().poLine(line).quantity(qty).build());
    });

    boolean complete = po.getLines().stream().allMatch(l -> l.getOutstanding() == 0);
    po.setStatus(complete ? Status.RECEIVED : Status.PARTIALLY_RECEIVED);
    purchaseOrderRepository.save(po);
    return toResponse(receiptRepository.save(receipt));
  }

  @Transactional
  public PurchaseOrderResponse cancel(UUID id) {
    PurchaseOrder po = find(id);
    requireStatus(po, "cancelled", Status.DRAFT, Status.ORDERED);
    po.setStatus(Status.CANCELLED);
    return toResponse(purchaseOrderRepository.save(po));
  }

  /** Stops waiting for the rest of a partially delivered order. */
  @Transactional
  public PurchaseOrderResponse close(UUID id) {
    PurchaseOrder po = find(id);
    requireStatus(po, "closed", Status.PARTIALLY_RECEIVED);
    po.setStatus(Status.CLOSED);
    return toResponse(purchaseOrderRepository.save(po));
  }

  @Transactional(readOnly = true)
  public PurchaseOrderResponse get(UUID id) {
    return toResponse(find(id));
  }

  @Transactional(readOnly = true)
  public Page<PurchaseOrderResponse> list(Status status, UUID supplierId, UUID warehouseId, Pageable pageable) {
    Specification<PurchaseOrder> spec = (root, query, cb) -> cb.conjunction();
    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    if (supplierId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("supplier").get("id"), supplierId));
    }
    if (warehouseId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("warehouse").get("id"), warehouseId));
    }
    return purchaseOrderRepository.findAll(spec, pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public List<GoodsReceiptResponse> listReceipts(UUID id) {
    find(id);
    return receiptRepository.findByPurchaseOrderIdOrderByReceivedAtAsc(id).stream().map(this::toResponse).toList();
  }

  // ---------- helpers ----------

  private void fillLines(PurchaseOrder po, List<PoLineRequest> requests) {
    Map<UUID, PoLineRequest> merged = new LinkedHashMap<>();
    for (PoLineRequest r : requests) {
      merged.merge(r.variantId(), r, (a, b) -> new PoLineRequest(a.variantId(), a.quantity() + b.quantity(),
          a.unitCost() != null ? a.unitCost() : b.unitCost()));
    }
    merged.values().forEach(r -> {
      ProductVariant variant = variantRepository.findById(r.variantId())
          .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + r.variantId()));
      var cost = r.unitCost() != null ? r.unitCost()
          : supplierProductRepository.findBySupplierIdAndVariantId(po.getSupplier().getId(), variant.getId())
              .map(sp -> sp.getUnitCost()).orElse(variant.getCostPrice());
      po.addLine(PurchaseOrderLine.builder().variant(variant).quantityOrdered(r.quantity()).unitCost(cost).build());
    });
  }

  private void requireStatus(PurchaseOrder po, String action, Status... allowed) {
    for (Status s : allowed) {
      if (po.getStatus() == s) {
        return;
      }
    }
    throw new IllegalStateException("Purchase order " + po.getPoNumber() + " is " + po.getStatus()
        + " and cannot be " + action);
  }

  private Warehouse activeWarehouse(UUID id) {
    Warehouse w = warehouseRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + id));
    if (!Boolean.TRUE.equals(w.getIsActive())) {
      throw new IllegalStateException("Warehouse is inactive: " + w.getCode());
    }
    return w;
  }

  private PurchaseOrder find(UUID id) {
    return purchaseOrderRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + id));
  }

  private PurchaseOrderResponse toResponse(PurchaseOrder po) {
    return new PurchaseOrderResponse(po.getId(), po.getPoNumber(), po.getSupplier().getId(),
        po.getSupplier().getName(), po.getWarehouse().getId(), po.getWarehouse().getCode(),
        po.getStatus().name(), po.getExpectedDate(), po.getNotes(), po.getTotalAmount(),
        po.getLines().stream().map(l -> new PoLineResponse(l.getId(), l.getVariant().getId(),
            l.getVariant().getSku(), l.getVariant().getProduct().getName(), l.getQuantityOrdered(),
            l.getQuantityReceived(), l.getOutstanding(), l.getUnitCost(), l.getLineTotal())).toList(),
        po.getOrderedAt(), po.getCreatedAt());
  }

  private GoodsReceiptResponse toResponse(GoodsReceipt r) {
    return new GoodsReceiptResponse(r.getId(), r.getReceiptNumber(), r.getNotes(),
        r.getLines().stream().map(l -> new ReceiptLineResponse(l.getPoLine().getId(),
            l.getPoLine().getVariant().getSku(), l.getQuantity())).toList(),
        r.getReceivedBy(), r.getReceivedAt());
  }
}
