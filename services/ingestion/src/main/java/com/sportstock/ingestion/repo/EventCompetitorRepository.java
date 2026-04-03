package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.EventCompetitor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventCompetitorRepository extends JpaRepository<EventCompetitor, Long> {

  Optional<EventCompetitor> findByEventIdAndTeamId(Long eventId, Long teamId);

  List<EventCompetitor> findByEventId(Long eventId);

  List<EventCompetitor> findByEventEspnId(String espnId);
}
