package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.EventCompetitorLinescore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventCompetitorLinescoreRepository extends JpaRepository<EventCompetitorLinescore, Long> {

    Optional<EventCompetitorLinescore> findByEventCompetitorIdAndPeriod(Long eventCompetitorId, Integer period);
}
