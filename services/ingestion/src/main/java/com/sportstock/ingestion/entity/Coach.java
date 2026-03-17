package com.sportstock.ingestion.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "coaches",
    schema = "ingestion",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_coaches_espn_team_season",
            columnNames = {"espn_id", "team_id", "season_year"}))
@Getter
@Setter
@NoArgsConstructor
public class Coach {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "espn_id", nullable = false, length = 15)
  private String espnId;

  @Column(name = "first_name", length = 100)
  private String firstName;

  @Column(name = "last_name", length = 100)
  private String lastName;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  private Team team;

  @Column(name = "season_year")
  private Integer seasonYear;

  @Column(name = "ingested_at", nullable = false)
  private Instant ingestedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
