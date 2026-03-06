package com.sportstock.league.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "league_members", schema = "league")
public class LeagueMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Positive
    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Pattern(regexp = "^(OWNER|MEMBER)$")
    @Size(max = 16)
    @NotNull
    @Column(name = "role", nullable = false, length = 16)
    private String role;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

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

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (joinedAt == null) joinedAt = now;
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

}
