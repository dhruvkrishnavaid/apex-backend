package com.apex.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.request.OrderRequests.CreateOrderRequest;
import com.apex.dto.request.OrderRequests.OrderLineRequest;
import com.apex.dto.response.OrderResponses.OrderLineResponse;
import com.apex.dto.response.OrderResponses.OrderResponse;
import com.apex.exception.ResourceNotFoundException;
import com.apex.model.Customer;
import com.apex.model.ProductVariant;
import com.apex.model.SalesOrder;
import com.apex.model.SalesOrder.Channel;
import com.apex.model.SalesOrder.Status;
import com.apex.model.SalesOrderLine;
import com.apex.model.Warehouse;
import com.apex.repository.CustomerRepository;
import com.apex.repository.ProductVariantRepository;
import com.apex.repository.SalesOrderRepository;
import com.apex.repository.WarehouseRepository;
import com.apex.security.CurrentUser;

import lombok.RequiredArgsConstructor;

/** Order capture. Fulfilment steps live in {@link FulfilmentService}. */
@Service
@RequiredArgsConstructor
public class OrderService {

  private final SalesOrderRepository orderRepository;
  private final CustomerRepository customerRepository;
  private final WarehouseRepository warehouseRepository;
  private final ProductVariantRepository variantRepository;
  private final InventoryService inventoryService;

  @Value("${app.sales.tax-rate:0}")
  private BigDecimal taxRate;

  /** Creates the order and reserves stock for every line; fails as a whole if any line cannot be covered. */
  @Transactional
  public OrderResponse create(CreateOrderRequest request) {
    Customer customer = null;
    if (request.customerId() != null) {
      customer = customerRepository.findById(request.customerId())
          .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));
      if (!Boolean.TRUE.equals(customer.getIsActive())) {
        throw new IllegalStateException("Customer is inactive");
      }
    }
    Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
        .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + request.warehouseId()));
    if (!Boolean.TRUE.equals(warehouse.getIsActive())) {
      throw new IllegalStateException("Warehouse is inactive: " + warehouse.getCode());
    }

    SalesOrder order = SalesOrder.builder()
        .orderNumber(ReferenceNumbers.next("SO"))
        .customer(customer)
        .warehouse(warehouse)
        .channel(request.channel() != null ? request.channel() : Channel.IN_STORE)
        .shippingAddress(request.shippingAddress())
        .notes(request.notes())
        .taxRate(taxRate)
        .createdBy(CurrentUser.id().orElse(null))
        .build();

    Map<UUID, OrderLineRequest> merged = new LinkedHashMap<>();
    for (OrderLineRequest l : request.lines()) {
      merged.merge(l.variantId(), l, (a, b) -> new OrderLineRequest(a.variantId(), a.quantity() + b.quantity(),
          a.unitPrice(), a.discountPercent()));
    }

    merged.values().forEach(l -> {
      ProductVariant variant = variantRepository.findById(l.variantId())
          .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + l.variantId()));
      if (!Boolean.TRUE.equals(variant.getIsActive()) || variant.getProduct().getDeletedAt() != null) {
        throw new IllegalStateException("SKU is not available for sale: " + variant.getSku());
      }
      inventoryService.reserve(variant.getId(), warehouse.getId(), l.quantity());
      order.addLine(SalesOrderLine.builder()
          .variant(variant)
          .quantity(l.quantity())
          .unitPrice(l.unitPrice() != null ? l.unitPrice() : variant.getRetailPrice())
          .unitCost(variant.getCostPrice())
          .discountPercent(l.discountPercent() != null ? l.discountPercent() : BigDecimal.ZERO)
          .build());
    });

    return toResponse(orderRepository.save(order));
  }

  @Transactional(readOnly = true)
  public OrderResponse get(UUID id) {
    return toResponse(find(id));
  }

  @Transactional(readOnly = true)
  public Page<OrderResponse> list(Status status, UUID customerId, UUID warehouseId, String orderNumber,
      java.time.LocalDate from, java.time.LocalDate to, Pageable pageable) {
    Specification<SalesOrder> spec = (root, query, cb) -> cb.conjunction();
    if (status != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
    }
    if (customerId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId));
    }
    if (warehouseId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("warehouse").get("id"), warehouseId));
    }
    if (orderNumber != null && !orderNumber.isBlank()) {
      String pattern = "%" + orderNumber.trim().toLowerCase(Locale.ROOT) + "%";
      spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("orderNumber")), pattern));
    }
    if (from != null) {
      spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
    }
    if (to != null) {
      spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
    }
    return orderRepository.findAll(spec, pageable).map(this::toResponse);
  }

  SalesOrder find(UUID id) {
    return orderRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
  }

  OrderResponse toResponse(SalesOrder o) {
    return new OrderResponse(o.getId(), o.getOrderNumber(),
        o.getCustomer() != null ? o.getCustomer().getId() : null,
        o.getCustomer() != null ? o.getCustomer().getName() : null,
        o.getWarehouse().getId(), o.getWarehouse().getCode(), o.getStatus().name(), o.getChannel().name(),
        o.getShippingAddress(), o.getNotes(), o.getSubtotal(), o.getTaxAmount(), o.getTotal(),
        o.getCarrier(), o.getTrackingNumber(),
        o.getLines().stream().map(l -> new OrderLineResponse(l.getId(), l.getVariant().getId(),
            l.getVariant().getSku(), l.getVariant().getProduct().getName(), l.getQuantity(),
            l.getQuantityPicked(), l.getQuantityReturned(), l.getUnitPrice(), l.getDiscountPercent(),
            l.getLineTotal())).toList(),
        o.getShippedAt(), o.getDeliveredAt(), o.getCreatedAt());
  }
}
