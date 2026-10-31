package com.apex.model;

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
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Current stock of one variant in one warehouse. */
@Entity
@Table(name = "stock_levels", uniqueConstraints = {
    @UniqueConstraint(name = "uk_stock_variant_warehouse", columnNames = { "variant_id", "warehouse_id" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockLevel {

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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "location_id")
  private StorageLocation location;

  @Builder.Default
  @Column(name = "quantity_on_hand", nullable = false)
  private Integer quantityOnHand = 0;

  /** Quantity promised to open orders / outgoing transfers; still physically in the warehouse. */
  @Builder.Default
  @Column(name = "quantity_reserved", nullable = false)
  private Integer quantityReserved = 0;

  @Version
  private Long version;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public int getAvailable() {
    return quantityOnHand - quantityReserved;
  }

  @PrePersist
  @PreUpdate
  protected void touch() {
    updatedAt = LocalDateTime.now();
  }
}
