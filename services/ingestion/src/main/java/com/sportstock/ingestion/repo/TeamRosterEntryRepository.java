package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.TeamRosterEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRosterEntryRepository extends JpaRepository<TeamRosterEntry, Long> {

  Optional<TeamRosterEntry> findByTeamIdAndAthleteIdAndSeasonYear(
      Long teamId, Long athleteId, Integer seasonYear);

  List<TeamRosterEntry> findByTeamEspnIdAndSeasonYear(String espnTeamId, Integer seasonYear);

  List<TeamRosterEntry> findByAthleteIdAndSeasonYear(Long athleteId, Integer seasonYear);

  @Query(
      """
      SELECT tre FROM TeamRosterEntry tre
      JOIN FETCH tre.team t
      JOIN FETCH tre.athlete a
      WHERE tre.seasonYear = :seasonYear
        AND tre.injuryStatus IS NOT NULL
        AND TRIM(tre.injuryStatus) <> ''
      """)
  List<TeamRosterEntry> findInjuredBySeasonYear(@Param("seasonYear") Integer seasonYear);
}
