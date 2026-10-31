package com.apex.model;

import java.time.LocalDateTime;
import java.util.UUID;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Immutable ledger entry: every change of quantity on hand is recorded here. */
@Entity
@Table(name = "stock_movements", indexes = {
    @Index(name = "idx_movements_variant", columnList = "variant_id"),
    @Index(name = "idx_movements_warehouse", columnList = "warehouse_id"),
    @Index(name = "idx_movements_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

  public enum MovementType {
    INITIAL, PURCHASE_RECEIPT, ADJUSTMENT, STOCK_COUNT, DAMAGE, TRANSFER_OUT, TRANSFER_IN, SALE, RETURN
  }

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private MovementType type;

  /** Signed change of quantity on hand. */
  @Column(name = "quantity_change", nullable = false)
  private Integer quantityChange;

  @Column(name = "quantity_after", nullable = false)
  private Integer quantityAfter;

  @Column(name = "reference_type", length = 40)
  private String referenceType;

  @Column(name = "reference_id", columnDefinition = "UUID")
  private UUID referenceId;

  @Column(length = 500)
  private String reason;

  @Column(name = "performed_by", columnDefinition = "UUID")
  private UUID performedBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
