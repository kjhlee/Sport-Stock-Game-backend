package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.PlayerGameStat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerGameStatRepository extends JpaRepository<PlayerGameStat, Long> {

  Optional<PlayerGameStat> findByEventIdAndAthleteEspnIdAndStatCategory(
      Long eventId, String athleteEspnId, String statCategory);

  List<PlayerGameStat> findByEventId(Long eventId);

  List<PlayerGameStat> findByAthleteId(Long athleteId);

  List<PlayerGameStat> findByEventIdAndTeamId(Long eventId, Long teamId);

  List<PlayerGameStat> findByEventIdAndAthleteEspnId(Long eventId, String athleteEspnId);
}
