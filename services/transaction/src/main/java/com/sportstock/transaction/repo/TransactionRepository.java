package com.sportstock.transaction.repo;

import com.sportstock.transaction.entity.Transaction;
import com.sportstock.transaction.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByUserIdAndLeagueIdOrderByCreatedAtDesc(Long userId, Long leagueId, Pageable pageable);

    Page<Transaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    Page<Transaction> findByLeagueIdAndType(Long leagueId, TransactionType type, Pageable pageable);
}
