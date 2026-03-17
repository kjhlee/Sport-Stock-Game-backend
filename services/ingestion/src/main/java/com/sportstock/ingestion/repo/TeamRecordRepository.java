package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.TeamRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRecordRepository extends JpaRepository<TeamRecord, Long> {

  Optional<TeamRecord> findByTeamIdAndSeasonYearAndRecordType(
      Long teamId, Integer seasonYear, String recordType);

  List<TeamRecord> findByTeamIdAndSeasonYear(Long teamId, Integer seasonYear);
}
