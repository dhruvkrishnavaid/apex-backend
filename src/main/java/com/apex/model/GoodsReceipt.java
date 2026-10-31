package com.apex.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One delivery against a purchase order (goods received note). */
@Entity
@Table(name = "goods_receipts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceipt {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "receipt_number", unique = true, nullable = false, length = 40)
  private String receiptNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "purchase_order_id", nullable = false)
  private PurchaseOrder purchaseOrder;

  @Column(length = 500)
  private String notes;

  @Column(name = "received_by", columnDefinition = "UUID")
  private UUID receivedBy;

  @Builder.Default
  @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<GoodsReceiptLine> lines = new ArrayList<>();

  @Column(name = "received_at", nullable = false, updatable = false)
  private LocalDateTime receivedAt;

  public void addLine(GoodsReceiptLine line) {
    lines.add(line);
    line.setReceipt(this);
  }

  @PrePersist
  protected void onCreate() {
    receivedAt = LocalDateTime.now();
  }
}
