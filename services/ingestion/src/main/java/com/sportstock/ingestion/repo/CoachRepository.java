package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.Coach;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachRepository extends JpaRepository<Coach, Long> {

  Optional<Coach> findByEspnIdAndTeamIdAndSeasonYear(
      String espnId, Long teamId, Integer seasonYear);
}
