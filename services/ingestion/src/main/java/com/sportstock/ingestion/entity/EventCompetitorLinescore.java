package com.sportstock.ingestion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "event_competitor_linescores", schema = "ingestion",
        uniqueConstraints = @UniqueConstraint(name = "uk_event_competitor_linescores_competitor_period",
                columnNames = {"event_competitor_id", "period"}))
@Getter
@Setter
@NoArgsConstructor
public class EventCompetitorLinescore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_competitor_id", nullable = false)
    private EventCompetitor eventCompetitor;

    @Column(nullable = false)
    private Integer period;

    @Column(precision = 5, scale = 1)
    private BigDecimal value;

    @Column(name = "display_value", length = 10)
    private String displayValue;
}
