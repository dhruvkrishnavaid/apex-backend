package com.apex.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.TransferRequests.CreateTransferRequest;
import com.apex.dto.request.TransferRequests.ReceiveTransferRequest;
import com.apex.dto.request.TransferRequests.ReceivedLine;
import com.apex.dto.request.TransferRequests.TransferLineRequest;
import com.apex.dto.response.TransferResponses.TransferLineResponse;
import com.apex.dto.response.TransferResponses.TransferResponse;
import com.apex.exception.BusinessRuleException;
import com.apex.exception.ResourceNotFoundException;
import com.apex.model.ProductVariant;
import com.apex.model.StockMovement.MovementType;
import com.apex.model.StockTransfer;
import com.apex.model.StockTransfer.Status;
import com.apex.model.StockTransferLine;
import com.apex.model.Warehouse;
import com.apex.repository.ProductVariantRepository;
import com.apex.repository.StockTransferRepository;
import com.apex.repository.WarehouseRepository;
import com.apex.security.CurrentUser;

import lombok.RequiredArgsConstructor;

/**
 * Transfer lifecycle: REQUESTED (stock reserved at source) -> IN_TRANSIT (stock leaves source)
 * -> RECEIVED (stock lands at destination). A REQUESTED transfer can be CANCELLED.
 */
@Service
@RequiredArgsConstructor
public class TransferService {

  private static final String REF = "TRANSFER";

  private final StockTransferRepository transferRepository;
  private final WarehouseRepository warehouseRepository;
  private final ProductVariantRepository variantRepository;
  private final InventoryService inventoryService;

  @Transactional
  public TransferResponse create(CreateTransferRequest request) {
    if (request.fromWarehouseId().equals(request.toWarehouseId())) {
      throw new IllegalArgumentException("Source and destination warehouse must differ");
    }
    Warehouse from = activeWarehouse(request.fromWarehouseId());
    Warehouse to = activeWarehouse(request.toWarehouseId());

    // merge duplicate variants so that reservation is done once per variant
    Map<UUID, Integer> quantities = new LinkedHashMap<>();
    for (TransferLineRequest line : request.lines()) {
      quantities.merge(line.variantId(), line.quantity(), Integer::sum);
    }

    StockTransfer transfer = StockTransfer.builder()
        .referenceNumber(ReferenceNumbers.next("TRF"))
        .fromWarehouse(from)
        .toWarehouse(to)
        .notes(request.notes())
        .requestedBy(CurrentUser.id().orElse(null))
        .build();

    quantities.forEach((variantId, qty) -> {
      ProductVariant variant = variantRepository.findById(variantId)
          .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));
      inventoryService.reserve(variantId, from.getId(), qty);
      transfer.addLine(StockTransferLine.builder().variant(variant).quantity(qty).build());
    });

    return toResponse(transferRepository.save(transfer));
  }

  @Transactional
  public TransferResponse dispatch(UUID id) {
    StockTransfer transfer = find(id);
    requireStatus(transfer, Status.REQUESTED, "dispatched");
    for (StockTransferLine line : transfer.getLines()) {
      inventoryService.consumeReserved(line.getVariant().getId(), transfer.getFromWarehouse().getId(),
          line.getQuantity(), MovementType.TRANSFER_OUT, REF, transfer.getId(), transfer.getReferenceNumber());
    }
    transfer.setStatus(Status.IN_TRANSIT);
    transfer.setDispatchedAt(LocalDateTime.now());
    return toResponse(transferRepository.save(transfer));
  }

  @Transactional
  public TransferResponse receive(UUID id, ReceiveTransferRequest request) {
    StockTransfer transfer = find(id);
    requireStatus(transfer, Status.IN_TRANSIT, "received");

    Map<UUID, Integer> overrides = new HashMap<>();
    if (request != null && request.lines() != null) {
      for (ReceivedLine rl : request.lines()) {
        overrides.put(rl.lineId(), rl.receivedQuantity());
      }
      for (UUID lineId : overrides.keySet()) {
        if (transfer.getLines().stream().noneMatch(l -> l.getId().equals(lineId))) {
          throw new IllegalArgumentException("Line does not belong to this transfer: " + lineId);
        }
      }
    }

    for (StockTransferLine line : transfer.getLines()) {
      int received = overrides.getOrDefault(line.getId(), line.getQuantity());
      if (received > line.getQuantity()) {
        throw new BusinessRuleException("Received quantity exceeds shipped quantity for SKU "
            + line.getVariant().getSku());
      }
      line.setReceivedQuantity(received);
      if (received > 0) {
        inventoryService.apply(line.getVariant().getId(), transfer.getToWarehouse().getId(), received,
            MovementType.TRANSFER_IN, REF, transfer.getId(), transfer.getReferenceNumber());
      }
    }
    transfer.setStatus(Status.RECEIVED);
    transfer.setReceivedAt(LocalDateTime.now());
    return toResponse(transferRepository.save(transfer));
  }

  @Transactional
  public TransferResponse cancel(UUID id) {
    StockTransfer transfer = find(id);
    requireStatus(transfer, Status.REQUESTED, "cancelled");
    for (StockTransferLine line : transfer.getLines()) {
      inventoryService.release(line.getVariant().getId(), transfer.getFromWarehouse().getId(), line.getQuantity());
    }
    transfer.setStatus(Status.CANCELLED);
    return toResponse(transferRepository.save(transfer));
  }

  @Transactional(readOnly = true)
  public TransferResponse get(UUID id) {
    return toResponse(find(id));
  }

  @Transactional(readOnly = true)
  public Page<TransferResponse> list(Status status, UUID warehouseId, Pageable pageable) {
    Specification<StockTransfer> spec = (root, query, cb) -> cb.conjunction();
    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    if (warehouseId != null) {
      spec = spec.and((root, query, cb) -> cb.or(
          cb.equal(root.get("fromWarehouse").get("id"), warehouseId),
          cb.equal(root.get("toWarehouse").get("id"), warehouseId)));
    }
    return transferRepository.findAll(spec, pageable).map(this::toResponse);
  }

  private void requireStatus(StockTransfer transfer, Status expected, String action) {
    if (transfer.getStatus() != expected) {
      throw new IllegalStateException("Transfer " + transfer.getReferenceNumber() + " is "
          + transfer.getStatus() + " and cannot be " + action);
    }
  }

  private Warehouse activeWarehouse(UUID id) {
    Warehouse w = warehouseRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + id));
    if (!Boolean.TRUE.equals(w.getIsActive())) {
      throw new IllegalStateException("Warehouse is inactive: " + w.getCode());
    }
    return w;
  }

  private StockTransfer find(UUID id) {
    return transferRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Transfer not found: " + id));
  }

  private TransferResponse toResponse(StockTransfer t) {
    return new TransferResponse(t.getId(), t.getReferenceNumber(),
        t.getFromWarehouse().getId(), t.getFromWarehouse().getCode(),
        t.getToWarehouse().getId(), t.getToWarehouse().getCode(),
        t.getStatus().name(), t.getNotes(),
        t.getLines().stream().map(l -> new TransferLineResponse(l.getId(), l.getVariant().getId(),
            l.getVariant().getSku(), l.getQuantity(), l.getReceivedQuantity())).toList(),
        t.getDispatchedAt(), t.getReceivedAt(), t.getCreatedAt());
  }
}
