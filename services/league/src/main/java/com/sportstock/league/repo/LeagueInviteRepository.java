package com.sportstock.league.repo;

import com.sportstock.league.entity.LeagueInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface LeagueInviteRepository extends JpaRepository<LeagueInvite, Long> {

    Optional<LeagueInvite> findByCode(String code);

    @Modifying
    @Query("UPDATE LeagueInvite i SET i.usesCount = i.usesCount + 1 WHERE i.id = :id AND (i.maxUses IS NULL OR i.usesCount < i.maxUses)")
    int incrementUsesCount(@Param("id") Long id);

    List<LeagueInvite> findByCreatedBy(Long createdBy);

    List<LeagueInvite> findByRevokedAtIsNull();

    List<LeagueInvite> findByExpiresAtBeforeAndRevokedAtIsNull(OffsetDateTime cutoff);

    Optional<LeagueInvite> findByCodeAndRevokedAtIsNull(String code);
}
