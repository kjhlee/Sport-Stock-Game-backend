package com.sportstock.ingestion.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teams", schema = "ingestion")
@Getter
@Setter
@NoArgsConstructor
public class Team {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "espn_id", nullable = false, unique = true, length = 15)
  private String espnId;

  @Column(name = "espn_uid", length = 50)
  private String espnUid;

  @Column(length = 100)
  private String slug;

  @Column(nullable = false, length = 10)
  private String abbreviation;

  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Column(name = "short_display_name", length = 50)
  private String shortDisplayName;

  @Column(length = 50)
  private String name;

  @Column(length = 50)
  private String nickname;

  @Column(length = 50)
  private String location;

  @Column(length = 10)
  private String color;

  @Column(name = "alternate_color", length = 10)
  private String alternateColor;

  @Column(name = "is_active")
  private Boolean isActive;

  @Column(name = "is_all_star")
  private Boolean isAllStar;

  @Column(name = "logo_url", length = 500)
  private String logoUrl;

  @Column(name = "franchise_id", length = 15)
  private String franchiseId;

  @Column(name = "division_id", length = 15)
  private String divisionId;

  @Column(name = "conference_id", length = 15)
  private String conferenceId;

  @Column(name = "standing_summary", length = 50)
  private String standingSummary;

  @Column(name = "ingested_at", nullable = false)
  private Instant ingestedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
