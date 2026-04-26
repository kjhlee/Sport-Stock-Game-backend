package com.sportstock.stockmarket.model.entity;

import com.sportstock.common.enums.stock_market.PriceType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "price_history", schema = "market")
@Getter
@Setter
@NoArgsConstructor
public class PriceHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "stock_id", nullable = false)
  private Stock stock;

  @Column(name = "season_year", nullable = false)
  private int seasonYear;

  @Column(name = "season_type", nullable = false)
  private int seasonType;

  @Column(name = "week", nullable = false)
  private int week;

  @Column(name = "price", nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Enumerated(EnumType.STRING)
  @Column(name = "price_type", nullable = false, length = 20)
  private PriceType priceType = PriceType.FINAL;

  @Column(name = "recorded_at", nullable = false, updatable = false)
  private Instant recordedAt;

  @PrePersist
  protected void onCreate() {
    this.recordedAt = Instant.now();
  }
}
