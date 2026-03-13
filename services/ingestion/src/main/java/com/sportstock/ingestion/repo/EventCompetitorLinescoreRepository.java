package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.EventCompetitorLinescore;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventCompetitorLinescoreRepository
    extends JpaRepository<EventCompetitorLinescore, Long> {

  Optional<EventCompetitorLinescore> findByEventCompetitorIdAndPeriod(
      Long eventCompetitorId, Integer period);
}
