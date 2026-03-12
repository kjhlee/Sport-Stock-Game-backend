package com.sportstocks.stockmarket.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sportstocks.stockmarket.dto.response.PagedStockResponse;
import com.sportstocks.stockmarket.dto.response.StockResponse;
import com.sportstocks.stockmarket.exception.StockNotFoundException;
import com.sportstocks.stockmarket.model.entity.PlayerStock;
import com.sportstocks.stockmarket.model.enums.StockStatus;
import com.sportstocks.stockmarket.repository.PlayerStockRepository;

@Service
public class StockQueryService {

    private final PlayerStockRepository playerStockRepository;

    public StockQueryService(PlayerStockRepository playerStockRepository) {
        this.playerStockRepository = playerStockRepository;
    }

    @Transactional(readOnly = true)
    public StockResponse getStock(UUID stockId) {
        PlayerStock stock = playerStockRepository.findById(stockId)
                .orElseThrow(() -> new StockNotFoundException(stockId));

        return toResponse(stock);
    }

    @Transactional(readOnly = true)
    public PagedStockResponse listStocks(String position, StockStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        String normalizedPosition = normalizePosition(position);
        Page<PlayerStock> stockPage;

        if (normalizedPosition != null && status != null) {
            stockPage = playerStockRepository.findByPositionAndStatus(normalizedPosition, status, pageable);
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
                stockPage.getTotalPages()
        );
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
                stock.getPriceUpdatedAt()
        );
    }
}