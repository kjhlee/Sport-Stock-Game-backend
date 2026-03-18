package com.sportstock.stockmarket.service;

import com.sportstock.common.dto.stock_market.PagedStockResponse;
import com.sportstock.common.dto.stock_market.PriceHistoryResponse;
import com.sportstock.common.dto.stock_market.StockResponse;
import com.sportstock.stockmarket.exception.StockNotFoundException;
import com.sportstock.stockmarket.model.entity.PlayerStock;
import com.sportstock.stockmarket.model.enums.StockStatus;
import com.sportstock.stockmarket.repository.PlayerStockRepository;
import com.sportstock.stockmarket.repository.PriceHistoryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockQueryService {

  private final PlayerStockRepository playerStockRepository;
  private final PriceHistoryRepository priceHistoryRepository;

  public StockQueryService(
      PlayerStockRepository playerStockRepository, PriceHistoryRepository priceHistoryRepository) {
    this.playerStockRepository = playerStockRepository;
    this.priceHistoryRepository = priceHistoryRepository;
  }

  @Transactional(readOnly = true)
  public StockResponse getStock(UUID stockId) {
    PlayerStock stock =
        playerStockRepository
            .findById(stockId)
            .orElseThrow(() -> new StockNotFoundException(stockId));

    return toResponse(stock);
  }

  @Transactional(readOnly = true)
  public StockResponse getStockByEspnId(String athleteEspnId) {
    PlayerStock stock =
        playerStockRepository
            .findByAthleteEspnId(athleteEspnId)
            .orElseThrow(() -> new StockNotFoundException((athleteEspnId)));

    return toResponse(stock);
  }

  @Transactional(readOnly = true)
  public PagedStockResponse listStocks(String position, StockStatus status, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);

    String normalizedPosition = normalizePosition(position);
    Page<PlayerStock> stockPage;

    if (normalizedPosition != null && status != null) {
      stockPage =
          playerStockRepository.findByPositionAndStatus(normalizedPosition, status, pageable);
    } else if (normalizedPosition != null) {
      stockPage = playerStockRepository.findByPosition(normalizedPosition, pageable);
    } else if (status != null) {
      stockPage = playerStockRepository.findByStatus(status, pageable);
    } else {
      stockPage = playerStockRepository.findAll(pageable);
    }

    return new PagedStockResponse(
        stockPage.getContent().stream().map(this::toResponse).toList(),
        stockPage.getNumber(),
        stockPage.getSize(),
        stockPage.getTotalElements(),
        stockPage.getTotalPages());
  }

  @Transactional(readOnly = true)
  public List<PriceHistoryResponse> getPriceHistory(UUID stockId, int seasonYear, int seasonType) {
    if (!playerStockRepository.existsById(stockId)) {
      throw new StockNotFoundException(stockId);
    }
    return priceHistoryRepository
        .findByPlayerStockIdAndSeasonYearAndSeasonTypeOrderByWeekAsc(
            stockId, seasonYear, seasonType)
        .stream()
        .map(
            h ->
                new PriceHistoryResponse(
                    h.getSeasonYear(),
                    h.getSeasonType(),
                    h.getWeek(),
                    h.getPrice(),
                    h.getRecordedAt()))
        .toList();
  }

  private String normalizePosition(String rawPosition) {
    if (rawPosition == null || rawPosition.isBlank()) {
      return null;
    }

    String normalized = rawPosition.trim().toUpperCase();

    if ("PK".equals(normalized)) {
      return "K";
    }

    return normalized;
  }

  private StockResponse toResponse(PlayerStock stock) {
    return new StockResponse(
        stock.getId(),
        stock.getAthleteEspnId(),
        stock.getFullName(),
        stock.getPosition(),
        stock.getTeamEspnId(),
        stock.getCurrentPrice(),
        stock.getStatus().name(),
        stock.getPriceUpdatedAt());
  }
}
