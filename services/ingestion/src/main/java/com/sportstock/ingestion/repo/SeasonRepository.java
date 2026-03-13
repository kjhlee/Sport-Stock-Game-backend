package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.Season;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRepository extends JpaRepository<Season, Long> {

  Optional<Season> findByYearAndSeasonTypeId(Integer year, String seasonTypeId);
}
