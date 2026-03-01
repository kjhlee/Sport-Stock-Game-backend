package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.Coach;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoachRepository extends JpaRepository<Coach, Long> {

    Optional<Coach> findByEspnIdAndTeamIdAndSeasonYear(String espnId, Long teamId, Integer seasonYear);
}
