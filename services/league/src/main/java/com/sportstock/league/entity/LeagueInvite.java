package com.sportstock.league.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "league_invites", schema = "league")
public class LeagueInvite {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @NotBlank
  @Size(max = 64)
  @NotNull
  @Column(name = "code", nullable = false, length = 64)
  private String code;

  @Positive
  @NotNull
  @Column(name = "created_by", nullable = false)
  private Long createdBy;

  @Column(name = "expires_at")
  private OffsetDateTime expiresAt;

  @Positive
  @Column(name = "max_uses")
  private Integer maxUses;

  @Min(0)
  @NotNull
  @ColumnDefault("0")
  @Column(name = "uses_count", nullable = false)
  private Integer usesCount;

  @Column(name = "revoked_at")
  private OffsetDateTime revokedAt;

  @NotNull
  @ColumnDefault("now()")
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @NotNull
  @ColumnDefault("now()")
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "league_id", nullable = false)
  private League league;

  @AssertTrue(message = "usesCount must be less than or equal to maxUses when maxUses is set")
  private boolean isUsageLimitValid() {
    if (maxUses == null || usesCount == null) {
      return true;
    }
    return usesCount <= maxUses;
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
