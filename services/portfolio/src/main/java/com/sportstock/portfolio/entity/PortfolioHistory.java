package com.sportstock.portfolio.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
    name = "portfolio_history",
    schema = "portfolio",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"user_id", "league_id", "week_number", "season_type"}))
public class PortfolioHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "portfolio_id", nullable = false)
  private Portfolio portfolio;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "league_id", nullable = false)
  private Long leagueId;

  @Column(name = "week_number", nullable = false)
  private Integer weekNumber;

  @Column(name = "season_type", nullable = false, length = 32)
  private String seasonType;

  @Column(name = "start_value", nullable = false, precision = 19, scale = 4)
  private BigDecimal startValue;

  @Column(name = "end_value", nullable = false, precision = 19, scale = 4)
  private BigDecimal endValue;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  void onCreate() {
    OffsetDateTime now = OffsetDateTime.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}
