package com.sportstock.ingestion.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sportstock.common.enums.stock_market.StockType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "fantasy_snapshot",
        schema = "ingestion",
        uniqueConstraints =
        @UniqueConstraint(
                name = "uk_fantasy_snapshot_event_subject",
                columnNames = {"event_id", "subject_type", "espn_id"}))
@Getter
@Setter
@NoArgsConstructor
public class FantasySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @JsonIgnore
    private Event event;

    @Column(name = "subject_type", nullable = false, length = 20)
    private String subjectType;

    @Column(name = "espn_id", nullable = false, length = 15)
    private String espnId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "projected_stats", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String projectedStats;

    @Column(name = "projected_fantasy_points", precision = 10, scale = 2)
    private BigDecimal projectedFantasyPoints;

    @Column(name = "actual_fantasy_points", precision = 10, scale = 2)
    private BigDecimal actualFantasyPoints;

    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.ingestedAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
