package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByEspnId(String espnId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.espnId = :espnId")
    Optional<Event> findByEspnIdWithLock(@Param("espnId") String espnId);

    List<Event> findBySeasonYearOrderByDateAsc(Integer seasonYear);

    List<Event> findBySeasonYearAndSeasonTypeAndWeekNumberOrderByDateAsc(
            Integer seasonYear,
            Integer seasonType,
            Integer weekNumber
    );

    List<Event> findBySeasonYearAndStatusCompletedTrue(Integer seasonYear);
}
