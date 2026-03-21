package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.Event;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

  Optional<Event> findByEspnId(String espnId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT e FROM Event e WHERE e.espnId = :espnId")
  Optional<Event> findByEspnIdWithLock(@Param("espnId") String espnId);

  List<Event> findBySeasonYearOrderByDateAsc(Integer seasonYear);

  List<Event> findBySeasonYearAndSeasonTypeAndWeekNumberOrderByDateAsc(
      Integer seasonYear, Integer seasonType, Integer weekNumber);

  List<Event> findBySeasonYearAndStatusCompletedTrue(Integer seasonYear);

  @Query("""
    SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Event e
    WHERE e.date >= :rangeStart
      AND e.date < :rangeEnd
      AND (e.statusCompleted = false OR e.statusCompleted IS NULL)
    """)
  boolean hasIncompleteEventsInRange(
          @Param("rangeStart") Instant rangeStart,
          @Param("rangeEnd") Instant rangeEnd);

  @Query("""
    SELECT e FROM Event e
    WHERE e.statusCompleted = true
      AND e.summaryIngestedAt IS NULL
      AND e.date >= :rangeStart
      AND e.date < :rangeEnd
    ORDER BY e.date ASC
    """)
  List<Event> findCompletedEventsNeedingSummary(
          @Param("rangeStart") Instant rangeStart,
          @Param("rangeEnd") Instant rangeEnd);
}
