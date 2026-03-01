package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.EventCompetitor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventCompetitorRepository extends JpaRepository<EventCompetitor, Long> {

    Optional<EventCompetitor> findByEventIdAndTeamId(Long eventId, Long teamId);

    List<EventCompetitor> findByEventId(Long eventId);
}
