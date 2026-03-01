package com.sportstock.ingestion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "seasons", schema = "ingestion",
        uniqueConstraints = @UniqueConstraint(name = "uk_seasons_year_type", columnNames = {"year", "season_type_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "display_name", length = 20)
    private String displayName;

    @Column(name = "season_type_id", length = 5)
    private String seasonTypeId;

    @Column(name = "season_type_name", length = 30)
    private String seasonTypeName;

    @Column(name = "season_type_abbreviation", length = 10)
    private String seasonTypeAbbreviation;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
