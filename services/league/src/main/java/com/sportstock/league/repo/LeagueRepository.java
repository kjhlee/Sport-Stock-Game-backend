package com.sportstock.league.repo;

import com.sportstock.league.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeagueRepository extends JpaRepository<League, Long> {

    List<League> findByOwnerUserId(Long ownerUserId);

    List<League> findByStatusOrderByCreatedAtDesc(String status);

    List<League> findByOwnerUserIdAndStatus(Long ownerUserId, String status);
}
