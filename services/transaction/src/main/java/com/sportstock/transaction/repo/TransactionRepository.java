package com.sportstock.transaction.repo;

import com.sportstock.transaction.entity.Transaction;
import com.sportstock.transaction.enums.TransactionType;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  Page<Transaction> findByUserIdAndLeagueIdOrderByCreatedAtDesc(
      Long userId, Long leagueId, Pageable pageable);

  Page<Transaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

  boolean existsByIdempotencyKey(String idempotencyKey);

  Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

  Page<Transaction> findByLeagueIdAndType(Long leagueId, TransactionType type, Pageable pageable);

  @Query(
      """
    SELECT COALESCE(SUM(t.amount / t.pricePerShare), 0)
    FROM Transaction t
    WHERE t.buyTransactionId = :buyTxId
      AND t.type IN (com.sportstock.transaction.enums.TransactionType.STOCK_SELL,
                     com.sportstock.transaction.enums.TransactionType.LIQUIDATE_ASSETS)
    """)
  BigDecimal sumSoldQuantityByBuyTransactionId(@Param("buyTxId") Long buyTransactionId);
}
