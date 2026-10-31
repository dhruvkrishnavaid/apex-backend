package com.apex.model;

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
@Table(name = "stock_transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransfer {

  public enum Status {
    REQUESTED, IN_TRANSIT, RECEIVED, CANCELLED
  }

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "reference_number", unique = true, nullable = false, length = 40)
  private String referenceNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "from_warehouse_id", nullable = false)
  private Warehouse fromWarehouse;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "to_warehouse_id", nullable = false)
  private Warehouse toWarehouse;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.REQUESTED;

  @Column(length = 500)
  private String notes;

  @Builder.Default
  @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<StockTransferLine> lines = new ArrayList<>();

  @Column(name = "requested_by", columnDefinition = "UUID")
  private UUID requestedBy;

  @Column(name = "dispatched_at")
  private LocalDateTime dispatchedAt;

  @Column(name = "received_at")
  private LocalDateTime receivedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public void addLine(StockTransferLine line) {
    lines.add(line);
    line.setTransfer(this);
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
