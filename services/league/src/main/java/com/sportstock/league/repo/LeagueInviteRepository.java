package com.sportstock.league.repo;

import com.sportstock.league.entity.LeagueInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface LeagueInviteRepository extends JpaRepository<LeagueInvite, Long> {

    Optional<LeagueInvite> findByCode(String code);

    List<LeagueInvite> findByCreatedBy(Long createdBy);

    List<LeagueInvite> findByRevokedAtIsNull();

    List<LeagueInvite> findByExpiresAtBeforeAndRevokedAtIsNull(OffsetDateTime cutoff);

    Optional<LeagueInvite> findByCodeAndRevokedAtIsNull(String code);
}
