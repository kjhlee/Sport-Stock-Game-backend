package com.sportstock.league.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Getter
@Setter
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

    @Pattern(regexp = "^(ACTIVE|INACTIVE)$")
    @Size(max = 16)
    @NotNull
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "joined_at", nullable = false)
    private OffsetDateTime joinedAt;

    @Column(name = "left_at")
    private OffsetDateTime leftAt;

    @Column(name = "removed_at")
    private OffsetDateTime removedAt;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

}
