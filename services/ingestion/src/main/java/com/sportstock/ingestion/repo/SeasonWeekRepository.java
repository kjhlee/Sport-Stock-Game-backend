package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.SeasonWeek;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeasonWeekRepository extends JpaRepository<SeasonWeek, Long> {

  Optional<SeasonWeek> findBySeasonIdAndSeasonTypeValueAndWeekValue(
      Long seasonId, String seasonTypeValue, String weekValue);

  @Query(
      """
    SELECT sw FROM SeasonWeek sw
    JOIN FETCH sw.season s
    WHERE sw.startDate <= :now
      AND sw.endDate >= :now
      AND sw.seasonTypeValue IN :seasonTypes
      AND s.year = :seasonYear
    ORDER BY sw.seasonTypeValue ASC
    """)
  Optional<SeasonWeek> findCurrentWeek(
      @Param("now") Instant now,
      @Param("seasonYear") Integer seasonYear,
      @Param("seasonTypes") List<String> seasonTypes);

  @Query(
      "SELECT CASE WHEN COUNT(sw) > 0 THEN true ELSE false END FROM SeasonWeek sw JOIN sw.season s WHERE s.year = :year")
  boolean existsBySeasonYear(@Param("year") Integer year);
}
