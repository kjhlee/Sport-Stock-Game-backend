package com.sportstock.league.repo;

import com.sportstock.league.entity.LeagueMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeagueMemberRepository extends JpaRepository<LeagueMember, Long> {

    Optional<LeagueMember> findByUserIdAndRole(Long userId, String role);

    List<LeagueMember> findByUserId(Long userId);

    Optional<LeagueMember> findByLeagueIdAndUserId(Long leagueId, Long userId);

    List<LeagueMember> findAllByLeagueId(Long leagueId);

    int countByLeagueId(Long leagueId);
}
