package com.sportstock.league.repo;

import com.sportstock.league.entity.League;
import com.sportstock.league.enums.LeagueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeagueRepository extends JpaRepository<League, Long> {

    List<League> findByOwnerUserId(Long ownerUserId);

    List<League> findByStatusOrderByCreatedAtDesc(LeagueStatus status);

    List<League> findByOwnerUserIdAndStatus(Long ownerUserId, LeagueStatus status);
}
