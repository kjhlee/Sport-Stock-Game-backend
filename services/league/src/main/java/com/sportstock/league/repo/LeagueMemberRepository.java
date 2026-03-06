package com.sportstock.league.repo;

import com.sportstock.league.entity.LeagueMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeagueMemberRepository extends JpaRepository<LeagueMember, Long> {

    Optional<LeagueMember> findByUserIdAndRole(Long userId, String role);

    List<LeagueMember> findByUserId(Long userId);

    List<LeagueMember> findByStatus(String status);

    List<LeagueMember> findByRoleAndStatus(String role, String status);

    Optional<LeagueMember> findByLeagueIdAndUserId(Long leagueId, Long userId);

    List<LeagueMember> findByLeagueIdAndStatus(Long leagueId, String status);

    List<LeagueMember> findByUserIdAndStatus(Long userId, String status);

    long countByLeagueIdAndStatus(Long leagueId, String status);
}
