package com.sportstock.ingestion.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "events", schema = "ingestion")
@Getter
@Setter
@NoArgsConstructor
public class Event {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "espn_id", nullable = false, unique = true, length = 15)
  private String espnId;

  @Column(name = "espn_uid", length = 60)
  private String espnUid;

  @Column(length = 200)
  private String name;

  @Column(name = "short_name", length = 50)
  private String shortName;

  @Column(nullable = false)
  private Instant date;

  @Column(name = "season_year", nullable = false)
  private Integer seasonYear;

  @Column(name = "season_type")
  private Integer seasonType;

  @Column(name = "season_slug", length = 30)
  private String seasonSlug;

  @Column(name = "week_number")
  private Integer weekNumber;

  private Integer attendance;

  @Column(name = "neutral_site")
  private Boolean neutralSite;

  @Column(name = "conference_competition")
  private Boolean conferenceCompetition;

  @Column(name = "play_by_play_available")
  private Boolean playByPlayAvailable;

  @Column(name = "status_state", length = 20)
  private String statusState;

  @Column(name = "status_completed")
  private Boolean statusCompleted;

  @Column(name = "status_description", length = 50)
  private String statusDescription;

  @Column(name = "status_period")
  private Integer statusPeriod;

  @Column(name = "status_clock", precision = 10, scale = 1)
  private BigDecimal statusClock;

  @Column(length = 100)
  private String broadcast;

  @Column(name = "note_headline", length = 200)
  private String noteHeadline;

  @Column(name = "ingested_at", nullable = false)
  private Instant ingestedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
