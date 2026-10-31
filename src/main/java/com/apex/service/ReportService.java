package com.apex.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apex.dto.response.ReportResponses.AuditLogResponse;
import com.apex.dto.response.ReportResponses.Dashboard;
import com.apex.dto.response.ReportResponses.LowStockRow;
import com.apex.dto.response.ReportResponses.SalesDay;
import com.apex.dto.response.ReportResponses.SalesReport;
import com.apex.dto.response.ReportResponses.StockValuationReport;
import com.apex.dto.response.ReportResponses.StockValuationRow;
import com.apex.dto.response.ReportResponses.TopProductRow;
import com.apex.model.AuditLog;
import com.apex.model.PurchaseOrder;
import com.apex.model.SalesOrder;
import com.apex.model.SalesOrderLine;
import com.apex.model.SalesReturn;
import com.apex.model.StockAlert;
import com.apex.model.StockLevel;
import com.apex.repository.AuditLogRepository;
import com.apex.repository.ProductRepository;
import com.apex.repository.PurchaseOrderRepository;
import com.apex.repository.SalesOrderRepository;
import com.apex.repository.SalesReturnRepository;
import com.apex.repository.StockAlertRepository;
import com.apex.repository.StockLevelRepository;
import com.apex.repository.WarehouseRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

  /** Orders that count as sales: they have left (or are about to leave) the warehouse and were not cancelled. */
  private static final EnumSet<SalesOrder.Status> REVENUE_STATUSES = EnumSet.of(
      SalesOrder.Status.SHIPPED, SalesOrder.Status.DELIVERED);

  private final StockLevelRepository stockLevelRepository;
  private final SalesOrderRepository orderRepository;
  private final SalesReturnRepository returnRepository;
  private final PurchaseOrderRepository purchaseOrderRepository;
  private final StockAlertRepository alertRepository;
  private final ProductRepository productRepository;
  private final WarehouseRepository warehouseRepository;
  private final AuditLogRepository auditLogRepository;

  public StockValuationReport stockValuation(UUID warehouseId) {
    List<StockLevel> levels = warehouseId == null
        ? stockLevelRepository.findAllInStock()
        : stockLevelRepository.findInStockByWarehouse(warehouseId);
    List<StockValuationRow> rows = levels.stream().map(s -> {
      BigDecimal qty = BigDecimal.valueOf(s.getQuantityOnHand());
      return new StockValuationRow(s.getVariant().getId(), s.getVariant().getSku(),
          s.getVariant().getProduct().getName(), s.getWarehouse().getId(), s.getWarehouse().getCode(),
          s.getQuantityOnHand(), s.getVariant().getCostPrice(), s.getVariant().getRetailPrice(),
          s.getVariant().getCostPrice().multiply(qty), s.getVariant().getRetailPrice().multiply(qty));
    }).toList();
    return new StockValuationReport(
        rows.stream().mapToLong(StockValuationRow::quantityOnHand).sum(),
        rows.stream().map(StockValuationRow::costValue).reduce(BigDecimal.ZERO, BigDecimal::add),
        rows.stream().map(StockValuationRow::retailValue).reduce(BigDecimal.ZERO, BigDecimal::add),
        rows);
  }

  public List<LowStockRow> lowStock(int threshold) {
    return stockLevelRepository.findAtOrBelowAvailable(threshold).stream()
        .map(s -> new LowStockRow(s.getVariant().getId(), s.getVariant().getSku(),
            s.getVariant().getProduct().getName(), s.getWarehouse().getId(), s.getWarehouse().getCode(),
            s.getQuantityOnHand(), s.getAvailable()))
        .toList();
  }

  public SalesReport sales(LocalDate from, LocalDate to) {
    List<SalesOrder> orders = salesOrders(from, to);
    Map<LocalDate, long[]> counters = new TreeMap<>();      // orders, units
    Map<LocalDate, BigDecimal[]> amounts = new TreeMap<>(); // revenue, profit

    long units = 0;
    BigDecimal revenue = BigDecimal.ZERO;
    BigDecimal cost = BigDecimal.ZERO;
    for (SalesOrder o : orders) {
      LocalDate day = o.getShippedAt() != null ? o.getShippedAt().toLocalDate() : o.getCreatedAt().toLocalDate();
      long orderUnits = o.getLines().stream().mapToLong(SalesOrderLine::getQuantity).sum();
      BigDecimal orderRevenue = o.getSubtotal();
      BigDecimal orderCost = o.getLines().stream()
          .map(l -> l.getUnitCost().multiply(BigDecimal.valueOf(l.getQuantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);

      long[] c = counters.computeIfAbsent(day, d -> new long[2]);
      c[0]++;
      c[1] += orderUnits;
      BigDecimal[] a = amounts.computeIfAbsent(day, d -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
      a[0] = a[0].add(orderRevenue);
      a[1] = a[1].add(orderRevenue.subtract(orderCost));

      units += orderUnits;
      revenue = revenue.add(orderRevenue);
      cost = cost.add(orderCost);
    }

    BigDecimal refunds = returnsBetween(from, to).stream().map(SalesReturn::getRefundAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    List<SalesDay> days = counters.entrySet().stream()
        .map(e -> new SalesDay(e.getKey(), e.getValue()[0], e.getValue()[1],
            amounts.get(e.getKey())[0], amounts.get(e.getKey())[1]))
        .toList();
    return new SalesReport(from, to, orders.size(), units, revenue, cost, revenue.subtract(cost), refunds,
        revenue.subtract(refunds), days);
  }

  public List<TopProductRow> topProducts(LocalDate from, LocalDate to, int limit) {
    Map<UUID, long[]> units = new HashMap<>();
    Map<UUID, BigDecimal> revenue = new HashMap<>();
    Map<UUID, SalesOrderLine> sample = new HashMap<>();
    for (SalesOrder o : salesOrders(from, to)) {
      for (SalesOrderLine l : o.getLines()) {
        UUID id = l.getVariant().getId();
        units.computeIfAbsent(id, k -> new long[1])[0] += l.getQuantity();
        revenue.merge(id, l.getLineTotal(), BigDecimal::add);
        sample.putIfAbsent(id, l);
      }
    }
    return units.entrySet().stream()
        .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
        .limit(Math.max(1, limit))
        .map(e -> {
          SalesOrderLine l = sample.get(e.getKey());
          return new TopProductRow(e.getKey(), l.getVariant().getSku(), l.getVariant().getProduct().getName(),
              e.getValue()[0], revenue.get(e.getKey()));
        })
        .collect(Collectors.toList());
  }

  public Dashboard dashboard() {
    LocalDate today = LocalDate.now();
    List<SalesOrder> todays = salesOrders(today, today);
    long pending = orderRepository.count((root, q, cb) -> cb.equal(root.get("status"), SalesOrder.Status.PENDING));
    long toShip = orderRepository.count((root, q, cb) -> root.get("status").in(
        SalesOrder.Status.PENDING, SalesOrder.Status.PICKING, SalesOrder.Status.PACKED));
    long openPos = purchaseOrderRepository.count((root, q, cb) -> root.get("status").in(
        PurchaseOrder.Status.ORDERED, PurchaseOrder.Status.PARTIALLY_RECEIVED));
    return new Dashboard(
        productRepository.countByDeletedAtIsNullAndIsActiveTrue(),
        warehouseRepository.countByIsActiveTrue(),
        stockLevelRepository.totalUnitsOnHand(),
        stockLevelRepository.totalCostValue(),
        alertRepository.countByStatus(StockAlert.Status.OPEN),
        pending,
        toShip,
        openPos,
        todays.size(),
        todays.stream().map(SalesOrder::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add),
        LocalDateTime.now());
  }

  public Page<AuditLogResponse> auditLogs(UUID userId, String method, String resource, LocalDate from, LocalDate to,
      Pageable pageable) {
    Specification<AuditLog> spec = (root, q, cb) -> cb.conjunction();
    if (userId != null) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("userId"), userId));
    }
    if (method != null && !method.isBlank()) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("method"), method.toUpperCase()));
    }
    if (resource != null && !resource.isBlank()) {
      spec = spec.and((root, q, cb) -> cb.equal(root.get("resource"), resource));
    }
    if (from != null) {
      spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
    }
    if (to != null) {
      spec = spec.and((root, q, cb) -> cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
    }
    return auditLogRepository.findAll(spec, pageable).map(a -> new AuditLogResponse(a.getId(), a.getUserId(),
        a.getUserEmail(), a.getMethod(), a.getPath(), a.getResource(), a.getStatusCode(), a.getIpAddress(),
        a.getCreatedAt()));
  }

  // ---------- helpers ----------

  private List<SalesOrder> salesOrders(LocalDate from, LocalDate to) {
    LocalDate start = from != null ? from : LocalDate.now().minusDays(30);
    LocalDate end = to != null ? to : LocalDate.now();
    Specification<SalesOrder> spec = (root, q, cb) -> root.get("status").in(REVENUE_STATUSES);
    spec = spec.and((root, q, cb) -> cb.and(
        cb.greaterThanOrEqualTo(root.get("shippedAt"), start.atStartOfDay()),
        cb.lessThan(root.get("shippedAt"), end.plusDays(1).atStartOfDay())));
    return orderRepository.findAll(spec);
  }

  private List<SalesReturn> returnsBetween(LocalDate from, LocalDate to) {
    LocalDateTime start = (from != null ? from : LocalDate.now().minusDays(30)).atStartOfDay();
    LocalDateTime end = (to != null ? to : LocalDate.now()).plusDays(1).atStartOfDay();
    return returnRepository.findAll().stream()
        .filter(r -> !r.getCreatedAt().isBefore(start) && r.getCreatedAt().isBefore(end))
        .toList();
  }
}
