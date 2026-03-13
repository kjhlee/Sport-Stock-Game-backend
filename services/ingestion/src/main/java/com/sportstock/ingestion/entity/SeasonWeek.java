package com.sportstock.ingestion.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "season_weeks",
    schema = "ingestion",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_season_weeks_season_type_week",
            columnNames = {"season_id", "season_type_value", "week_value"}))
@Getter
@Setter
@NoArgsConstructor
public class SeasonWeek {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "season_id", nullable = false)
  private Season season;

  @Column(name = "season_type_value", nullable = false, length = 5)
  private String seasonTypeValue;

  @Column(name = "week_value", nullable = false, length = 5)
  private String weekValue;

  @Column(length = 50)
  private String label;

  @Column(name = "alternate_label", length = 50)
  private String alternateLabel;

  @Column(length = 50)
  private String detail;

  @Column(name = "start_date")
  private Instant startDate;

  @Column(name = "end_date")
  private Instant endDate;

  @Column(name = "ingested_at", nullable = false)
  private Instant ingestedAt;
}
