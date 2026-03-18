package com.sportstock.stockmarket.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
  @JoinColumn(name = "player_stock_id", nullable = false)
  private PlayerStock playerStock;

  @Column(name = "season_year", nullable = false)
  private int seasonYear;

  // 1 = preseason, 2 = regular, 3 = postseason
  @Column(name = "season_type", nullable = false)
  private int seasonType;

  @Column(name = "week", nullable = false)
  private int week;

  @Column(name = "price", nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Column(name = "recorded_at", nullable = false, updatable = false)
  private Instant recordedAt;

  @PrePersist
  protected void onCreate() {
    this.recordedAt = Instant.now();
  }
}
