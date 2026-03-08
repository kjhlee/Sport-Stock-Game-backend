package com.sportstocks.stockmarket.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.sportstocks.stockmarket.model.enums.StockStatus;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "player_stock", schema="market")
@Getter
@Setter
@NoArgsConstructor
public class PlayerStock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "athlete_espn_id", nullable = false, unique = true, length = 15)
    private String athleteEspnId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "position", nullable = false, length = 10)
    private String position;

    @Column(name = "team_espn_id", length = 15)
    private String teamEspnId;

    @Column(name = "current_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentPrice = BigDecimal.ZERO;

    // ACTIVE, INACTIVE, DELISTED
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StockStatus status = StockStatus.ACTIVE;

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
