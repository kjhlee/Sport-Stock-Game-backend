package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.Coach;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoachRepository extends JpaRepository<Coach, Long> {

  Optional<Coach> findByEspnIdAndTeamIdAndSeasonYear(
      String espnId, Long teamId, Integer seasonYear);

  @Modifying
  @Query(
      """
      DELETE FROM Coach c
      WHERE c.team.id = :teamId
        AND c.seasonYear = :seasonYear
        AND c.espnId NOT IN :coachEspnIds
      """)
  int deleteMissingByTeamAndSeasonYear(
      @Param("teamId") Long teamId,
      @Param("seasonYear") Integer seasonYear,
      @Param("coachEspnIds") List<String> coachEspnIds);
}
