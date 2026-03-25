package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.Team;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team, Long> {

  Optional<Team> findByEspnId(String espnId);

  List<Team> findAllByOrderByDisplayNameAsc();

  @Query(
      """
    SELECT t FROM Team t
    WHERE t.rosterSyncedAt IS NULL
       OR t.rosterSyncedAt < :cutoff
    ORDER BY t.displayName ASC
    """)
  List<Team> findTeamsNeedingRosterSync(@Param("cutoff") Instant cutoff);
}
