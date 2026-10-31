package com.apex.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.OrderRequests.CreateReturnRequest;
import com.apex.dto.request.OrderRequests.ReturnLineRequest;
import com.apex.dto.request.OrderRequests.ShipOrderRequest;
import com.apex.dto.response.OrderResponses.OrderResponse;
import com.apex.dto.response.OrderResponses.ReturnLineResponse;
import com.apex.dto.response.OrderResponses.ReturnResponse;
import com.apex.exception.BusinessRuleException;
import com.apex.model.SalesOrder;
import com.apex.model.SalesOrder.Status;
import com.apex.model.SalesOrderLine;
import com.apex.model.SalesReturn;
import com.apex.model.SalesReturnLine;
import com.apex.model.StockMovement.MovementType;
import com.apex.repository.SalesOrderRepository;
import com.apex.repository.SalesReturnRepository;
import com.apex.security.CurrentUser;

import lombok.RequiredArgsConstructor;

/**
 * Order fulfilment: PENDING -> PICKING -> PACKED -> SHIPPED -> DELIVERED.
 * Stock stays reserved until the order ships, then leaves the warehouse. Orders can be
 * cancelled until they ship; after that goods come back through a return.
 */
@Service
@RequiredArgsConstructor
public class FulfilmentService {

  private static final String REF = "SALES_ORDER";

  private final OrderService orderService;
  private final SalesOrderRepository orderRepository;
  private final SalesReturnRepository returnRepository;
  private final InventoryService inventoryService;

  /** Records picked quantities; with no lines given everything on the order is picked. */
  @Transactional
  public OrderResponse pick(UUID orderId, Map<UUID, Integer> picks) {
    SalesOrder order = orderService.find(orderId);
    requireStatus(order, "picked", Status.PENDING, Status.PICKING);

    if (picks == null || picks.isEmpty()) {
      order.getLines().forEach(l -> l.setQuantityPicked(l.getQuantity()));
    } else {
      picks.forEach((lineId, qty) -> {
        SalesOrderLine line = order.getLines().stream().filter(l -> l.getId().equals(lineId)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Line does not belong to this order: " + lineId));
        if (qty < 0 || qty > line.getQuantity()) {
          throw new BusinessRuleException("Picked quantity for SKU " + line.getVariant().getSku()
              + " must be between 0 and " + line.getQuantity());
        }
        line.setQuantityPicked(qty);
      });
    }
    order.setStatus(Status.PICKING);
    return orderService.toResponse(orderRepository.save(order));
  }

  @Transactional
  public OrderResponse pack(UUID orderId) {
    SalesOrder order = orderService.find(orderId);
    requireStatus(order, "packed", Status.PICKING);
    for (SalesOrderLine line : order.getLines()) {
      if (line.getQuantityPicked() < line.getQuantity()) {
        throw new BusinessRuleException("SKU " + line.getVariant().getSku() + " is not fully picked ("
            + line.getQuantityPicked() + "/" + line.getQuantity() + ")");
      }
    }
    order.setStatus(Status.PACKED);
    return orderService.toResponse(orderRepository.save(order));
  }

  @Transactional
  public OrderResponse ship(UUID orderId, ShipOrderRequest request) {
    SalesOrder order = orderService.find(orderId);
    requireStatus(order, "shipped", Status.PACKED);
    for (SalesOrderLine line : order.getLines()) {
      inventoryService.consumeReserved(line.getVariant().getId(), order.getWarehouse().getId(),
          line.getQuantity(), MovementType.SALE, REF, order.getId(), order.getOrderNumber());
    }
    order.setStatus(Status.SHIPPED);
    order.setShippedAt(LocalDateTime.now());
    if (request != null) {
      order.setCarrier(request.carrier());
      order.setTrackingNumber(request.trackingNumber());
    }
    return orderService.toResponse(orderRepository.save(order));
  }

  @Transactional
  public OrderResponse deliver(UUID orderId) {
    SalesOrder order = orderService.find(orderId);
    requireStatus(order, "marked delivered", Status.SHIPPED);
    order.setStatus(Status.DELIVERED);
    order.setDeliveredAt(LocalDateTime.now());
    return orderService.toResponse(orderRepository.save(order));
  }

  @Transactional
  public OrderResponse cancel(UUID orderId, String reason) {
    SalesOrder order = orderService.find(orderId);
    requireStatus(order, "cancelled", Status.PENDING, Status.PICKING, Status.PACKED);
    for (SalesOrderLine line : order.getLines()) {
      inventoryService.release(line.getVariant().getId(), order.getWarehouse().getId(), line.getQuantity());
    }
    order.setStatus(Status.CANCELLED);
    order.setCancelledAt(LocalDateTime.now());
    if (reason != null && !reason.isBlank()) {
      String existing = order.getNotes() == null ? "" : order.getNotes() + "\n";
      order.setNotes(existing + "Cancelled: " + reason);
    }
    return orderService.toResponse(orderRepository.save(order));
  }

  // ---------- Returns ----------

  @Transactional
  public ReturnResponse createReturn(UUID orderId, CreateReturnRequest request) {
    SalesOrder order = orderService.find(orderId);
    requireStatus(order, "returned", Status.SHIPPED, Status.DELIVERED);
    boolean restock = request.restock() == null || request.restock();

    Map<UUID, Integer> quantities = new HashMap<>();
    for (ReturnLineRequest l : request.lines()) {
      quantities.merge(l.orderLineId(), l.quantity(), Integer::sum);
    }

    SalesReturn salesReturn = SalesReturn.builder()
        .returnNumber(ReferenceNumbers.next("RET"))
        .order(order)
        .reason(request.reason())
        .restocked(restock)
        .refundAmount(BigDecimal.ZERO)
        .createdBy(CurrentUser.id().orElse(null))
        .build();

    BigDecimal total = BigDecimal.ZERO;
    for (Map.Entry<UUID, Integer> e : quantities.entrySet()) {
      SalesOrderLine line = order.getLines().stream().filter(l -> l.getId().equals(e.getKey())).findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Line does not belong to this order: " + e.getKey()));
      int qty = e.getValue();
      int returnable = line.getQuantity() - line.getQuantityReturned();
      if (qty > returnable) {
        throw new BusinessRuleException("Cannot return " + qty + " of SKU " + line.getVariant().getSku()
            + ": only " + returnable + " returnable");
      }
      BigDecimal net = line.getLineTotal()
          .multiply(BigDecimal.valueOf(qty))
          .divide(BigDecimal.valueOf(line.getQuantity()), 2, RoundingMode.HALF_UP);
      BigDecimal refund = net.add(net.multiply(order.getTaxRate())).setScale(2, RoundingMode.HALF_UP);
      line.setQuantityReturned(line.getQuantityReturned() + qty);
      salesReturn.addLine(SalesReturnLine.builder().orderLine(line).quantity(qty).refundAmount(refund).build());
      total = total.add(refund);

      if (restock) {
        inventoryService.apply(line.getVariant().getId(), order.getWarehouse().getId(), qty,
            MovementType.RETURN, "SALES_RETURN", order.getId(), request.reason());
      }
    }
    salesReturn.setRefundAmount(total);
    orderRepository.save(order);
    return toResponse(returnRepository.save(salesReturn));
  }

  @Transactional(readOnly = true)
  public List<ReturnResponse> listReturns(UUID orderId) {
    orderService.find(orderId);
    return returnRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream().map(this::toResponse).toList();
  }

  // ---------- helpers ----------

  private void requireStatus(SalesOrder order, String action, Status... allowed) {
    for (Status s : allowed) {
      if (order.getStatus() == s) {
        return;
      }
    }
    throw new IllegalStateException("Order " + order.getOrderNumber() + " is " + order.getStatus()
        + " and cannot be " + action);
  }

  private ReturnResponse toResponse(SalesReturn r) {
    return new ReturnResponse(r.getId(), r.getReturnNumber(), r.getOrder().getId(), r.getOrder().getOrderNumber(),
        r.getReason(), r.isRestocked(), r.getRefundAmount(),
        r.getLines().stream().map(l -> new ReturnLineResponse(l.getOrderLine().getId(),
            l.getOrderLine().getVariant().getSku(), l.getQuantity(), l.getRefundAmount())).toList(),
        r.getCreatedAt());
  }
}
