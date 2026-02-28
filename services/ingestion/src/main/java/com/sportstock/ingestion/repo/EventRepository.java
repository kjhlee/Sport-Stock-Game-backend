package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByEspnId(String espnId);

    List<Event> findBySeasonYearOrderByDateAsc(Integer seasonYear);

    List<Event> findBySeasonYearAndWeekNumber(Integer seasonYear, Integer weekNumber);

    List<Event> findBySeasonYearAndStatusCompletedTrue(Integer seasonYear);
}
