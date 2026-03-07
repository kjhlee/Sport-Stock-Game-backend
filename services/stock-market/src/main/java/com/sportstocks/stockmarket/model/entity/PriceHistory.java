package com.sportstocks.stockmarket.model.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "price_history", schema="market")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA requires a no-arg constructor
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // References the player stock this price belongs to.
    // LAZY fetch because we usually query price_history rows directly (by stock ID), not navigating from a PlayerStock.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_stock_id", nullable = false)
    private PlayerStock playerStock;

    // The season year this price corresponds to (e.g., 2024).
    @Column(name = "season_year", nullable = false)
    private int seasonYear;

    // 1 = preseason, 2 = regular, 3 = postseason
    @Column(name = "season_type", nullable = false)
    private int seasonType;

    // The week number this price corresponds to (1-17 for regular season).
    @Column(name = "week", nullable = false)
    private int week;

    // The player's stock price as of this week.
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    // When this row was written. Managed by @PrePersist.
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @PrePersist
    protected void onCreate() {
        this.recordedAt = Instant.now();
    }

}
