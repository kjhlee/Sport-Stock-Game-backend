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

@Entity
@Table(name = "player_stock", schema="market")
public class PlayerStock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ESPN's unique identifier for this athlete.
    @Column(name = "athlete_espn_id", nullable = false, unique = true, length = 15)
    private String athleteEspnId;

    // Athlete's full name - Potential duplicates.
    // Denormalized from ingestion, refreshed via sync-athletes.
    @Column(name = "full_name", nullable = false)
    private String fullName;

    // Position abbreviation (QB, RB, WR, TE, K).
    @Column(name = "position", nullable = false, length = 10)
    private String position;

    // Current team's ESPN ID. Nullable — a free agent has no team.
    // Denormalized from ingestion, refreshed via sync-athletes.
    @Column(name = "team_espn_id", length = 15)
    private String teamEspnId;

    // The latest calculated stock price.
    @Column(name = "current_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentPrice = BigDecimal.ZERO;

    // Controls whether this stock can be traded.
    // ACTIVE = normal trading, SUSPENDED = temporarily untradeable (e.g., injury), DELISTED = removed from market.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StockStatus status = StockStatus.ACTIVE;

    // When the price was last recalculated. Null until the first recalculation runs.
    @Column(name = "price_updated_at")
    private Instant priceUpdatedAt;

    // Managed by @PrePersist — set once on creation, never changed.
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Managed by @PrePersist and @PreUpdate — tracks the last modification to this row (any field, not just price).
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

    // Default constructor required by JPA. Not for application use.
    protected PlayerStock() {}

    // --- Getters and setters ---

    public UUID getId() { return id; }

    public String getAthleteEspnId() { return athleteEspnId; }
    public void setAthleteEspnId(String athleteEspnId) { this.athleteEspnId = athleteEspnId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getTeamEspnId() { return teamEspnId; }
    public void setTeamEspnId(String teamEspnId) { this.teamEspnId = teamEspnId; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public StockStatus getStatus() { return status; }
    public void setStatus(StockStatus status) { this.status = status; }

    public Instant getPriceUpdatedAt() { return priceUpdatedAt; }
    public void setPriceUpdatedAt(Instant priceUpdatedAt) { this.priceUpdatedAt = priceUpdatedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

}
