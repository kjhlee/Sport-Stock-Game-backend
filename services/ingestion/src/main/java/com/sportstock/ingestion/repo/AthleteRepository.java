package com.sportstock.ingestion.repo;

import com.sportstock.ingestion.entity.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AthleteRepository extends JpaRepository<Athlete, Long> {

    Optional<Athlete> findByEspnId(String espnId);

    List<Athlete> findByEspnIdIn(Collection<String> espnIds);

    List<Athlete> findByPositionAbbreviation(String positionAbbreviation);

    List<Athlete> findAllByOrderByFullNameAsc();

    List<Athlete> findByPositionAbbreviationOrderByFullNameAsc(String positionAbbreviation);
}
