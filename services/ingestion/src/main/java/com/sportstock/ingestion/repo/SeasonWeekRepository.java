package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.SeasonWeek;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeasonWeekRepository extends JpaRepository<SeasonWeek, Long> {

    Optional<SeasonWeek> findBySeasonIdAndSeasonTypeValueAndWeekValue(Long seasonId, String seasonTypeValue, String weekValue);
}
