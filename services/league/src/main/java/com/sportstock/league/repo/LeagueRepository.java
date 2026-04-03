package com.sportstock.league.repo;

import com.sportstock.common.enums.league.InitialStipendStatus;
import com.sportstock.common.enums.league.LeagueStatus;
import com.sportstock.league.entity.League;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeagueRepository extends JpaRepository<League, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT l FROM League l WHERE l.id = :id")
  Optional<League> findByIdForUpdate(@Param("id") Long id);

  List<League> findByOwnerUserId(Long ownerUserId);

  List<League> findByStatusOrderByCreatedAtDesc(LeagueStatus status);

  List<League> findByOwnerUserIdAndStatus(Long ownerUserId, LeagueStatus status);

  List<League> findByStatus(LeagueStatus status);

  List<League> findByStatusAndInitialStipendStatus(
      LeagueStatus status, InitialStipendStatus initialStipendStatus);
}
