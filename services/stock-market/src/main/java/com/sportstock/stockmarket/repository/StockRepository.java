package com.sportstock.stockmarket.repository;

import com.sportstock.common.enums.stock_market.StockStatus;
import com.sportstock.common.enums.stock_market.StockType;
import com.sportstock.stockmarket.model.entity.Stock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StockRepository extends JpaRepository<Stock, UUID> {

  Optional<Stock> findByEspnIdAndType(String espnId, StockType type);

  Page<Stock> findByStatus(StockStatus status, Pageable pageable);

  Page<Stock> findByPosition(String position, Pageable pageable);

  Page<Stock> findByPositionAndStatus(String position, StockStatus status, Pageable pageable);

  List<Stock> findByEspnIdIn(List<String> espnIds);

  List<Stock> findByStatusAndGameLockedFalse(StockStatus status);

  @Modifying
  @Query("UPDATE Stock s SET s.gameLocked = false")
  int unlockAllGameLocks();

  @Modifying
  @Query("UPDATE Stock s SET s.gameLocked = true WHERE s.espnId IN :espnIds")
  int lockByEspnIds(@Param("espnIds") List<String> espnIds);

  @Modifying
  @Query(
      "UPDATE Stock s SET s.injuryLocked = true WHERE s.espnId IN :espnIds AND s.type = 'PLAYER'")
  int setInjuryLockedByEspnIds(@Param("espnIds") List<String> espnIds);

  @Modifying
  @Query(
      "UPDATE Stock s SET s.injuryLocked = false WHERE s.espnId IN :espnIds AND s.type = 'PLAYER'")
  int clearInjuryLockedByEspnIds(@Param("espnIds") List<String> espnIds);

  Optional<Stock> findByEspnId(String espnId);
}
