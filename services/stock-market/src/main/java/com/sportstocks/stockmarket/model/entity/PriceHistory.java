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

@Entity
@Table(name = "price_history", schema="market")
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

    protected PriceHistory() {}

    // --- Getters and setters ---

    public UUID getId() { return id; }

    public PlayerStock getPlayerStock() { return playerStock; }
    public void setPlayerStock(PlayerStock playerStock) { this.playerStock = playerStock; }

    public int getSeasonYear() { return seasonYear; }
    public void setSeasonYear(int seasonYear) { this.seasonYear = seasonYear; }

    public int getSeasonType() { return seasonType; }
    public void setSeasonType(int seasonType) { this.seasonType = seasonType; }

    public int getWeek() { return week; }
    public void setWeek(int week) { this.week = week; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Instant getRecordedAt() { return recordedAt; }
    
}
