package com.sportstock.ingestion.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "boxscore_team_stats",
    schema = "ingestion",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_boxscore_team_stats_event_team_stat",
            columnNames = {"event_id", "team_id", "stat_name"}))
@Getter
@Setter
@NoArgsConstructor
public class BoxscoreTeamStat {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", nullable = false)
  @JsonIgnore
  private Event event;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id", nullable = false)
  @JsonIgnore
  private Team team;

  @Column(name = "home_away", nullable = false, length = 5)
  private String homeAway;

  @Column(name = "stat_name", nullable = false, length = 50)
  private String statName;

  @Column(name = "stat_value", length = 20)
  private String statValue;

  @Column(name = "display_value", length = 20)
  private String displayValue;

  @Column(length = 50)
  private String label;
}
