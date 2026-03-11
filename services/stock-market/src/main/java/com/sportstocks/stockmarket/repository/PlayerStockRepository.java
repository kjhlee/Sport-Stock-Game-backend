package com.sportstocks.stockmarket.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sportstocks.stockmarket.model.entity.PlayerStock;
import com.sportstocks.stockmarket.model.enums.StockStatus;

@Repository
public interface PlayerStockRepository extends JpaRepository<PlayerStock, UUID> {

    Optional<PlayerStock> findByAthleteEspnId(String athleteEspnId);

    Page<PlayerStock> findByStatus(StockStatus status, Pageable pageable);

    Page<PlayerStock> findByPosition(String position, Pageable pageable);

    Page<PlayerStock> findByPositionAndStatus(String position, StockStatus status, Pageable pageable);
}
