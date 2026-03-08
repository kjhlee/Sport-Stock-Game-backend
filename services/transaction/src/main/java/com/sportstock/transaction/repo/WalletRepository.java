package com.sportstock.transaction.repo;

import com.sportstock.transaction.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserIdAndLeagueId(Long userId, Long leagueId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Wallet> findByUserIdAndLeagueIdForUpdate(Long userId, Long leagueId);

    List<Wallet> findAllByLeagueId(Long leagueId);

    boolean existsByUserIdAndLeagueId(Long userId, Long leagueId);
}
