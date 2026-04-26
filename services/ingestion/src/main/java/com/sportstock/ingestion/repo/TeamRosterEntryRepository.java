package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.TeamRosterEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRosterEntryRepository extends JpaRepository<TeamRosterEntry, Long> {

  Optional<TeamRosterEntry> findByTeamIdAndAthleteIdAndSeasonYear(
      Long teamId, Long athleteId, Integer seasonYear);

  List<TeamRosterEntry> findByTeamEspnIdAndSeasonYear(String espnTeamId, Integer seasonYear);

  @Query(
      """
      SELECT a.espnId
      FROM TeamRosterEntry tre
      JOIN tre.team t
      JOIN tre.athlete a
      WHERE t.espnId = :espnTeamId
        AND tre.seasonYear = :seasonYear
      """)
  List<String> findAthleteEspnIdsByTeamEspnIdAndSeasonYear(
      @Param("espnTeamId") String espnTeamId, @Param("seasonYear") Integer seasonYear);

  List<TeamRosterEntry> findByAthleteIdAndSeasonYear(Long athleteId, Integer seasonYear);

  @Query(
      """
      SELECT tre FROM TeamRosterEntry tre
      JOIN FETCH tre.team t
      WHERE tre.athlete.id = :athleteId
      ORDER BY tre.seasonYear DESC, tre.updatedAt DESC
      """)
  List<TeamRosterEntry> findLatestByAthleteId(@Param("athleteId") Long athleteId);

  @Modifying
  @Query(
      """
      DELETE FROM TeamRosterEntry tre
      WHERE tre.team.id = :teamId
        AND tre.seasonYear = :seasonYear
        AND tre.athlete.espnId NOT IN :athleteEspnIds
      """)
  int deleteMissingByTeamAndSeasonYear(
      @Param("teamId") Long teamId,
      @Param("seasonYear") Integer seasonYear,
      @Param("athleteEspnIds") List<String> athleteEspnIds);

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
