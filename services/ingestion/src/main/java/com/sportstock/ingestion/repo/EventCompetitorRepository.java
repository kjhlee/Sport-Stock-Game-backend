package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.EventCompetitor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventCompetitorRepository extends JpaRepository<EventCompetitor, Long> {

  Optional<EventCompetitor> findByEventIdAndTeamId(Long eventId, Long teamId);

  List<EventCompetitor> findByEventId(Long eventId);

  List<EventCompetitor> findByEventEspnId(String espnId);

  @Query(
      """
      select ec
      from EventCompetitor ec
      join fetch ec.team
      where ec.event.espnId = :espnId
      """)
  List<EventCompetitor> findByEventEspnIdWithTeam(@Param("espnId") String espnId);
}
