package com.sportstock.scheduler.repo;

import com.sportstock.scheduler.entity.EventState;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventStateRepository extends JpaRepository<EventState, String> {
    List<EventState> findByStatus(String status);
    List<EventState> findByWeekNumberAndSeasonYearAndSeasonType(int weekNumber, int seasonYear, int seasonType);
}