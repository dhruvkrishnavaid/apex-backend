package com.apex.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A variant that a supplier can deliver, with the agreed unit cost. */
@Entity
@Table(name = "supplier_products", uniqueConstraints = {
    @UniqueConstraint(name = "uk_supplier_variant", columnNames = { "supplier_id", "variant_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierProduct {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "supplier_id", nullable = false)
  private Supplier supplier;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @Column(name = "supplier_sku", length = 100)
  private String supplierSku;

  @Column(name = "unit_cost", precision = 12, scale = 2, nullable = false)
  private BigDecimal unitCost;

  @Builder.Default
  @Column(name = "min_order_quantity", nullable = false)
  private Integer minOrderQuantity = 1;

  @Builder.Default
  @Column(name = "is_preferred", nullable = false)
  private Boolean isPreferred = false;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

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
