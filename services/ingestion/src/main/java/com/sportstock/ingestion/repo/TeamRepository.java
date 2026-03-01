package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    Optional<Team> findByEspnId(String espnId);

    List<Team> findAllByOrderByDisplayNameAsc();
}
