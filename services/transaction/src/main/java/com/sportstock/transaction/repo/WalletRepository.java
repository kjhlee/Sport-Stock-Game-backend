package com.sportstock.transaction.repo;

import com.sportstock.transaction.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserIdAndLeagueId(Long userId, Long leagueId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId AND w.leagueId = :leagueId")
    Optional<Wallet> findByUserIdAndLeagueIdForUpdate(@Param("userId") Long userId,
                                                      @Param("leagueId") Long leagueId);

    List<Wallet> findAllByLeagueId(Long leagueId);

    boolean existsByUserIdAndLeagueId(Long userId, Long leagueId);
}
