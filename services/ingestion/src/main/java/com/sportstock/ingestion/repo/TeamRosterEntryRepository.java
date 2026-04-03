package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.TeamRosterEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRosterEntryRepository extends JpaRepository<TeamRosterEntry, Long> {

  Optional<TeamRosterEntry> findByTeamIdAndAthleteIdAndSeasonYear(
      Long teamId, Long athleteId, Integer seasonYear);

  List<TeamRosterEntry> findByTeamEspnIdAndSeasonYear(String espnTeamId, Integer seasonYear);

  List<TeamRosterEntry> findByAthleteIdAndSeasonYear(Long athleteId, Integer seasonYear);


}
