package com.sportstock.ingestion.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "team_records", schema = "ingestion",
        uniqueConstraints = @UniqueConstraint(name = "uk_team_records_team_season_record_type",
                columnNames = {"team_id", "season_year", "record_type"}))
@Getter
@Setter
@NoArgsConstructor
public class TeamRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "season_year", nullable = false)
    private Integer seasonYear;

    @Column(name = "record_type", nullable = false, length = 20)
    private String recordType;

    @Column(length = 20)
    private String summary;

    private Integer wins;

    private Integer losses;

    private Integer ties;

    @Column(name = "win_percent", precision = 7, scale = 4)
    private BigDecimal winPercent;

    @Column(name = "ot_wins")
    private Integer otWins;

    @Column(name = "ot_losses")
    private Integer otLosses;

    @Column(name = "points_for")
    private Integer pointsFor;

    @Column(name = "points_against")
    private Integer pointsAgainst;

    @Column(name = "point_differential")
    private Integer pointDifferential;

    @Column(name = "avg_points_for", precision = 7, scale = 2)
    private BigDecimal avgPointsFor;

    @Column(name = "avg_points_against", precision = 7, scale = 2)
    private BigDecimal avgPointsAgainst;

    @Column(name = "playoff_seed")
    private Integer playoffSeed;

    private Integer streak;

    @Column(name = "games_played")
    private Integer gamesPlayed;

    @Column(name = "division_wins")
    private Integer divisionWins;

    @Column(name = "division_losses")
    private Integer divisionLosses;

    @Column(name = "division_ties")
    private Integer divisionTies;

    @Column(name = "league_win_percent", precision = 7, scale = 4)
    private BigDecimal leagueWinPercent;

    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
