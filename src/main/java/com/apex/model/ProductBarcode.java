package com.apex.model;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_barcodes", indexes = {
    @Index(name = "idx_product_barcodes_barcode", columnList = "barcode")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductBarcode {

  @Id
  @GeneratedValue
  @Column(columnDefinition = "UUID", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @Column(unique = true, nullable = false, length = 128)
  private String barcode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private BarcodeType type;

  @Builder.Default
  @Column(name = "is_primary", nullable = false)
  private Boolean isPrimary = true;

  public enum BarcodeType {
    UPC_A, EAN_13, CODE_128, QR_CODE
  }
}
