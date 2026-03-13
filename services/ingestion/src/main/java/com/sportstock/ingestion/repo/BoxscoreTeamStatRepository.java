package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.BoxscoreTeamStat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoxscoreTeamStatRepository extends JpaRepository<BoxscoreTeamStat, Long> {

  Optional<BoxscoreTeamStat> findByEventIdAndTeamIdAndStatName(
      Long eventId, Long teamId, String statName);

  List<BoxscoreTeamStat> findByEventId(Long eventId);
}
