package com.sportstock.ingestion.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "team_roster_entries",
    schema = "ingestion",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_team_roster_entries_team_athlete_season",
            columnNames = {"team_id", "athlete_id", "season_year"}))
@Getter
@Setter
@NoArgsConstructor
public class TeamRosterEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id", nullable = false)
  private Team team;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "athlete_id", nullable = false)
  private Athlete athlete;

  @Column(name = "season_year", nullable = false)
  private Integer seasonYear;

  @Column(name = "roster_group", nullable = false, length = 30)
  private String rosterGroup;

  @Column(length = 5)
  private String jersey;

  @Column(name = "position_abbreviation", length = 10)
  private String positionAbbreviation;

  @Column(name = "status_type", length = 30)
  private String statusType;

  @Column(name = "injury_status", length = 30)
  private String injuryStatus;

  @Column(name = "injury_date")
  private Instant injuryDate;

  @Column(name = "ingested_at", nullable = false)
  private Instant ingestedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
