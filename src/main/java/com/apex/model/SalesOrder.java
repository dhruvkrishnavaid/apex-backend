package com.apex.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sales_orders", indexes = {
    @Index(name = "idx_sales_orders_status", columnList = "status"),
    @Index(name = "idx_sales_orders_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrder {

  public enum Status {
    PENDING, PICKING, PACKED, SHIPPED, DELIVERED, CANCELLED
  }

  public enum Channel {
    IN_STORE, ONLINE, PHONE
  }

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "order_number", unique = true, nullable = false, length = 40)
  private String orderNumber;

  /** Null for anonymous walk-in sales. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id")
  private Customer customer;

  /** Warehouse that fulfils (and reserves stock for) the order. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.PENDING;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Channel channel = Channel.IN_STORE;

  @Column(name = "shipping_address", length = 500)
  private String shippingAddress;

  @Column(length = 1000)
  private String notes;

  @Builder.Default
  @Column(name = "tax_rate", precision = 6, scale = 4, nullable = false)
  private BigDecimal taxRate = BigDecimal.ZERO;

  @Builder.Default
  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SalesOrderLine> lines = new ArrayList<>();

  @Column(name = "tracking_number", length = 100)
  private String trackingNumber;

  @Column(length = 100)
  private String carrier;

  @Column(name = "created_by", columnDefinition = "UUID")
  private UUID createdBy;

  @Column(name = "shipped_at")
  private LocalDateTime shippedAt;

  @Column(name = "delivered_at")
  private LocalDateTime deliveredAt;

  @Column(name = "cancelled_at")
  private LocalDateTime cancelledAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public void addLine(SalesOrderLine line) {
    lines.add(line);
    line.setOrder(this);
  }

  public BigDecimal getSubtotal() {
    return lines.stream().map(SalesOrderLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public BigDecimal getTaxAmount() {
    return getSubtotal().multiply(taxRate).setScale(2, java.math.RoundingMode.HALF_UP);
  }

  public BigDecimal getTotal() {
    return getSubtotal().add(getTaxAmount());
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
