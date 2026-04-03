package com.sportstock.stockmarket.service;

import com.sportstock.common.dto.stock_market.PagedStockResponse;
import com.sportstock.common.dto.stock_market.PriceHistoryResponse;
import com.sportstock.common.dto.stock_market.StockResponse;
import com.sportstock.common.enums.stock_market.StockStatus;
import com.sportstock.common.enums.stock_market.StockType;
import com.sportstock.stockmarket.exception.StockNotFoundException;
import com.sportstock.stockmarket.model.entity.Stock;
import com.sportstock.stockmarket.repository.PriceHistoryRepository;
import com.sportstock.stockmarket.repository.StockRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockQueryService {

  private final StockRepository stockRepository;
  private final PriceHistoryRepository priceHistoryRepository;

  public StockQueryService(
      StockRepository stockRepository, PriceHistoryRepository priceHistoryRepository) {
    this.stockRepository = stockRepository;
    this.priceHistoryRepository = priceHistoryRepository;
  }

  @Transactional(readOnly = true)
  public StockResponse getStock(UUID stockId) {
    Stock stock =
        stockRepository.findById(stockId).orElseThrow(() -> new StockNotFoundException(stockId));

    return toResponse(stock);
  }

  @Transactional(readOnly = true)
  public StockResponse getStockByEspnId(String espnId, StockType type) {
    Stock stock =
        stockRepository
            .findByEspnIdAndType(espnId, type)
            .orElseThrow(() -> new StockNotFoundException(espnId));

    return toResponse(stock);
  }

  @Transactional(readOnly = true)
  public PagedStockResponse listStocks(String position, StockStatus status, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);

    String normalizedPosition = normalizePosition(position);
    Page<Stock> stockPage;

    if (normalizedPosition != null && status != null) {
      stockPage = stockRepository.findByPositionAndStatus(normalizedPosition, status, pageable);
    } else if (normalizedPosition != null) {
      stockPage = stockRepository.findByPosition(normalizedPosition, pageable);
    } else if (status != null) {
      stockPage = stockRepository.findByStatus(status, pageable);
    } else {
      stockPage = stockRepository.findAll(pageable);
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
    if (!stockRepository.existsById(stockId)) {
      throw new StockNotFoundException(stockId);
    }
    return priceHistoryRepository
        .findByStockIdAndSeasonYearAndSeasonTypeOrderByWeekAsc(stockId, seasonYear, seasonType)
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

  private StockResponse toResponse(Stock stock) {
    return new StockResponse(
        stock.getId(),
        stock.getEspnId(),
        stock.getFullName(),
        stock.getPosition(),
        stock.getType().name(),
        stock.getTeamEspnId(),
        stock.getCurrentPrice(),
        stock.getStatus().name(),
        stock.isGameLocked(),
        stock.isInjuryLocked(),
        stock.getPriceUpdatedAt());
  }
}
