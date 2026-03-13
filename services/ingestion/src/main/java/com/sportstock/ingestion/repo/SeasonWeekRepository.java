package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.SeasonWeek;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonWeekRepository extends JpaRepository<SeasonWeek, Long> {

  Optional<SeasonWeek> findBySeasonIdAndSeasonTypeValueAndWeekValue(
      Long seasonId, String seasonTypeValue, String weekValue);
}
