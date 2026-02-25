package com.sportstock.ingestion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "event_competitors", schema = "ingestion",
        uniqueConstraints = @UniqueConstraint(name = "uk_event_competitors_event_team",
                columnNames = {"event_id", "team_id"}))
@Getter
@Setter
@NoArgsConstructor
public class EventCompetitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "home_away", nullable = false, length = 5)
    private String homeAway;

    @Column(name = "\"order\"")
    private Integer order;

    private Boolean winner;

    @Column(length = 10)
    private String score;

    @Column(name = "record_summary", length = 20)
    private String recordSummary;

    @Column(name = "home_record", length = 20)
    private String homeRecord;

    @Column(name = "road_record", length = 20)
    private String roadRecord;

    @Column(name = "raw_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String rawJson;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;
}
