package com.sportstock.ingestion.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "player_game_stats",
    schema = "ingestion",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_player_game_stats_event_athlete_category",
            columnNames = {"event_id", "athlete_espn_id", "stat_category"}))
@Getter
@Setter
@NoArgsConstructor
public class PlayerGameStat {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", nullable = false)
  @JsonIgnore
  private Event event;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "athlete_id", nullable = false)
  @JsonIgnore
  private Athlete athlete;

  @Column(name = "athlete_espn_id", nullable = false, length = 15)
  private String athleteEspnId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id", nullable = false)
  @JsonIgnore
  private Team team;

  @Column(name = "stat_category", nullable = false, length = 30)
  private String statCategory;

  @Column(nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private String stats;

  @Column(name = "ingested_at", nullable = false)
  private Instant ingestedAt;
}
