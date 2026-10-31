package com.apex.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "purchase_order_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderLine {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "purchase_order_id", nullable = false)
  private PurchaseOrder purchaseOrder;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @Column(name = "quantity_ordered", nullable = false)
  private Integer quantityOrdered;

  @Builder.Default
  @Column(name = "quantity_received", nullable = false)
  private Integer quantityReceived = 0;

  @Column(name = "unit_cost", precision = 12, scale = 2, nullable = false)
  private BigDecimal unitCost;

  public int getOutstanding() {
    return quantityOrdered - quantityReceived;
  }

  public BigDecimal getLineTotal() {
    return unitCost.multiply(BigDecimal.valueOf(quantityOrdered));
  }
}
