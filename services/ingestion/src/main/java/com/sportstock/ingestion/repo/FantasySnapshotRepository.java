package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.FantasySnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FantasySnapshotRepository extends JpaRepository<FantasySnapshot, Long> {

  Optional<FantasySnapshot> findByEventIdAndSubjectTypeAndEspnId(
      Long eventId, String subjectType, String espnId);

  List<FantasySnapshot> findByEventId(Long eventId);

  @Query(
      """
      SELECT fs FROM FantasySnapshot fs
      JOIN fs.event e
      WHERE e.espnId = :eventEspnId
      """)
  List<FantasySnapshot> findByEventEspnId(@Param("eventEspnId") String eventEspnId);

  @Query(
      """
      SELECT fs FROM FantasySnapshot fs
      JOIN fs.event e
      WHERE e.espnId = :eventEspnId AND fs.completed = false
      """)
  List<FantasySnapshot> findIncompleteByEventEspnId(@Param("eventEspnId") String eventEspnId);

  @Modifying
  @Query(
      """
      UPDATE FantasySnapshot fs SET fs.completed = true, fs.updatedAt = CURRENT_TIMESTAMP
      WHERE fs.event.id IN (SELECT e.id FROM Event e WHERE e.espnId = :eventEspnId)
      """)
  int markCompletedByEventEspnId(@Param("eventEspnId") String eventEspnId);

  @Query(
      """
      SELECT fs FROM FantasySnapshot fs
      JOIN fs.event e
      WHERE fs.espnId = :espnId
        AND fs.subjectType = :subjectType
        AND e.seasonYear = :seasonYear
        AND e.seasonType = :seasonType
        AND e.weekNumber = :weekNumber
      """)
  Optional<FantasySnapshot> findByEspnIdAndSubjectTypeAndWeek(
      @Param("espnId") String espnId,
      @Param("subjectType") String subjectType,
      @Param("seasonYear") int seasonYear,
      @Param("seasonType") int seasonType,
      @Param("weekNumber") int weekNumber);
}
