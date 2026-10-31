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
@Table(name = "sales_order_lines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderLine {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private SalesOrder order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @Column(nullable = false)
  private Integer quantity;

  @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
  private BigDecimal unitPrice;

  /** Cost price at the time of sale, for margin reporting. */
  @Column(name = "unit_cost", precision = 12, scale = 2, nullable = false)
  private BigDecimal unitCost;

  @Builder.Default
  @Column(name = "discount_percent", precision = 5, scale = 2, nullable = false)
  private BigDecimal discountPercent = BigDecimal.ZERO;

  @Builder.Default
  @Column(name = "quantity_picked", nullable = false)
  private Integer quantityPicked = 0;

  @Builder.Default
  @Column(name = "quantity_returned", nullable = false)
  private Integer quantityReturned = 0;

  public BigDecimal getLineTotal() {
    BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity));
    BigDecimal factor = BigDecimal.ONE.subtract(discountPercent.divide(BigDecimal.valueOf(100), 6, java.math.RoundingMode.HALF_UP));
    return gross.multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);
  }
}
