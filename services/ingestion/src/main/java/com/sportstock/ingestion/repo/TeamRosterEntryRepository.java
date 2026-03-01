package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.TeamRosterEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRosterEntryRepository extends JpaRepository<TeamRosterEntry, Long> {

    Optional<TeamRosterEntry> findByTeamIdAndAthleteIdAndSeasonYear(Long teamId, Long athleteId, Integer seasonYear);

    List<TeamRosterEntry> findByTeamIdAndSeasonYear(Long teamId, Integer seasonYear);

    List<TeamRosterEntry> findByAthleteIdAndSeasonYear(Long athleteId, Integer seasonYear);
}
