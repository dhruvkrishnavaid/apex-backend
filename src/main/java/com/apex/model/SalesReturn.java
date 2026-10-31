package com.apex.model;

import java.math.BigDecimal;
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

@Entity
@Table(name = "sales_returns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReturn {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "return_number", unique = true, nullable = false, length = 40)
  private String returnNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private SalesOrder order;

  @Column(length = 500)
  private String reason;

  /** Whether returned goods went back into sellable stock (false = written off). */
  @Column(nullable = false)
  private boolean restocked;

  @Column(name = "refund_amount", precision = 12, scale = 2, nullable = false)
  private BigDecimal refundAmount;

  @Builder.Default
  @OneToMany(mappedBy = "salesReturn", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SalesReturnLine> lines = new ArrayList<>();

  @Column(name = "created_by", columnDefinition = "UUID")
  private UUID createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public void addLine(SalesReturnLine line) {
    lines.add(line);
    line.setSalesReturn(this);
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
