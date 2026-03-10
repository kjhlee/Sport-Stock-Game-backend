package com.sportstock.league.repo;

import com.sportstock.league.entity.LeagueMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeagueMemberRepository extends JpaRepository<LeagueMember, Long> {

    Optional<LeagueMember> findByUserIdAndRole(Long userId, String role);

    List<LeagueMember> findByUserId(Long userId);

    Page<LeagueMember> findByUserId(Long userId, Pageable pageable);

    Optional<LeagueMember> findByLeagueIdAndUserId(Long leagueId, Long userId);

    List<LeagueMember> findAllByLeagueId(Long leagueId);

    Page<LeagueMember> findAllByLeagueId(Long leagueId, Pageable pageable);

    int countByLeagueId(Long leagueId);

    @Query("SELECT lm.league.id, COUNT(lm) FROM LeagueMember lm WHERE lm.league.id IN :leagueIds GROUP BY lm.league.id")
    List<Object[]> countByLeagueIds(@Param("leagueIds") List<Long> leagueIds);
}
