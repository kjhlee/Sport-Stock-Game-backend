package com.sportstock.scheduler.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "event_state", schema = "scheduler")
@Getter
@Setter
@NoArgsConstructor
public class EventState {

    @Id
    @Column(name = "event_espn_id", length = 50)
    private String eventEspnId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Column(name = "week_number", nullable = false)
    private int weekNumber;

    @Column(name = "season_year", nullable = false)
    private int seasonYear;

    @Column(name = "season_type", nullable = false)
    private int seasonType;

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