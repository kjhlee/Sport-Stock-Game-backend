package com.sportstock.stockmarket.model.entity;

import com.sportstock.common.enums.stock_market.StockStatus;
import com.sportstock.common.enums.stock_market.StockType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock", schema = "market")
@Getter
@Setter
@NoArgsConstructor
public class Stock {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "espn_id", nullable = false, length = 15)
  private String espnId;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(name = "position", nullable = false, length = 10)
  private String position;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private StockType type;

  @Column(name = "team_espn_id", length = 15)
  private String teamEspnId;

  @Column(name = "current_price", nullable = false, precision = 10, scale = 2)
  private BigDecimal currentPrice = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private StockStatus status = StockStatus.ACTIVE;

  @Column(name = "game_locked", nullable = false)
  private boolean gameLocked = false;

  @Column(name = "injury_locked", nullable = false)
  private boolean injuryLocked = false;

  @Column(name = "price_updated_at")
  private Instant priceUpdatedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  protected void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }
}