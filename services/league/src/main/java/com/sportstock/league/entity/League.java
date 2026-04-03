package com.sportstock.league.entity;

import com.sportstock.common.enums.league.InitialStipendStatus;
import com.sportstock.common.enums.league.LeagueStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "leagues", schema = "league")
public class League {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Positive
  @NotNull
  @Column(name = "owner_user_id", nullable = false)
  private Long ownerUserId;

  @NotBlank
  @Size(max = 255)
  @NotNull
  @Column(name = "name", nullable = false)
  private String name;

  @NotNull
  @Enumerated(EnumType.STRING)
  @ColumnDefault("'INACTIVE'")
  @Column(name = "status", nullable = false, length = 16)
  private LeagueStatus status;

  @Min(2)
  @NotNull
  @Column(name = "max_members", nullable = false)
  private Integer maxMembers;

  @NotNull
  @Column(name = "season_start_at", nullable = false)
  private OffsetDateTime seasonStartAt;

  @NotNull
  @Column(name = "season_end_at", nullable = false)
  private OffsetDateTime seasonEndAt;

  @DecimalMin(value = "0.0", inclusive = false)
  @NotNull
  @Column(name = "initial_stipend_amount", nullable = false, precision = 19, scale = 4)
  private BigDecimal initialStipendAmount;

  @DecimalMin(value = "0.0", inclusive = false)
  @NotNull
  @Column(name = "weekly_stipend_amount", nullable = false, precision = 19, scale = 4)
  private BigDecimal weeklyStipendAmount;

  @Column(name = "started_at")
  private OffsetDateTime startedAt;

  @Column(name = "initial_stipend_issued_at")
  private OffsetDateTime initialStipendIssuedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "initial_stipend_status", nullable = false, length = 20)
  private InitialStipendStatus initialStipendStatus = InitialStipendStatus.NOT_APPLICABLE;

  @NotNull
  @ColumnDefault("now()")
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @NotNull
  @ColumnDefault("now()")
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @OneToMany(mappedBy = "league", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<LeagueMember> members = new ArrayList<>();

  @OneToMany(mappedBy = "league", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<LeagueInvite> invites = new ArrayList<>();

  @AssertTrue(message = "seasonEndAt must be after seasonStartAt")
  private boolean isSeasonRangeValid() {
    if (seasonStartAt == null || seasonEndAt == null) {
      return true;
    }
    return seasonEndAt.isAfter(seasonStartAt);
  }

  @PrePersist
  protected void onCreate() {
    OffsetDateTime now = OffsetDateTime.now();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }
}
