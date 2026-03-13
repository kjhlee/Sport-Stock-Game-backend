package com.sportstocks.stockmarket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sportstocks.stockmarket.dto.response.PriceHistoryResponse;
import com.sportstocks.stockmarket.exception.StockNotFoundException;
import com.sportstocks.stockmarket.model.entity.PlayerStock;
import com.sportstocks.stockmarket.model.entity.PriceHistory;
import com.sportstocks.stockmarket.model.enums.StockStatus;
import com.sportstocks.stockmarket.repository.PlayerStockRepository;
import com.sportstocks.stockmarket.repository.PriceHistoryRepository;

@ExtendWith(MockitoExtension.class)
class StockQueryServiceTest {

    @Mock private PlayerStockRepository playerStockRepository;
    @Mock private PriceHistoryRepository priceHistoryRepository;

    @InjectMocks private StockQueryService stockQueryService;

    // --- helpers ---

    private PlayerStock stock(UUID id, String athleteId, String position) {
        PlayerStock s = new PlayerStock();
        ReflectionTestUtils.setField(s, "id", id);
        s.setAthleteEspnId(athleteId);
        s.setFullName("Test Player");
        s.setPosition(position);
        s.setCurrentPrice(new BigDecimal("15.00"));
        s.setStatus(StockStatus.ACTIVE);
        return s;
    }

    private PriceHistory priceHistory(PlayerStock stock, int week, BigDecimal price) {
        PriceHistory h = new PriceHistory();
        h.setPlayerStock(stock);
        h.setSeasonYear(2024);
        h.setSeasonType(2);
        h.setWeek(week);
        h.setPrice(price);
        ReflectionTestUtils.setField(h, "recordedAt", Instant.now());
        return h;
    }

    // --- getPriceHistory ---

    @Test
    void getPriceHistory_returnsNonEmptyList_whenHistoryExists() {
        UUID stockId = UUID.randomUUID();
        PlayerStock s = stock(stockId, "qb-1", "QB");

        when(playerStockRepository.existsById(stockId)).thenReturn(true);
        when(priceHistoryRepository.findByPlayerStockIdAndSeasonYearAndSeasonTypeOrderByWeekAsc(stockId, 2024, 2))
                .thenReturn(List.of(
                        priceHistory(s, 1, new BigDecimal("18.12")),
                        priceHistory(s, 2, new BigDecimal("20.87")),
                        priceHistory(s, 3, new BigDecimal("22.45"))
                ));

        List<PriceHistoryResponse> result = stockQueryService.getPriceHistory(stockId, 2024, 2);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).week()).isEqualTo(1);
        assertThat(result.get(0).price()).isEqualByComparingTo("18.12");
        assertThat(result.get(1).week()).isEqualTo(2);
        assertThat(result.get(1).price()).isEqualByComparingTo("20.87");
        assertThat(result.get(2).week()).isEqualTo(3);
        assertThat(result.get(2).price()).isEqualByComparingTo("22.45");
    }

    @Test
    void getPriceHistory_returnsEmptyList_whenNoHistoryRecorded() {
        UUID stockId = UUID.randomUUID();

        when(playerStockRepository.existsById(stockId)).thenReturn(true);
        when(priceHistoryRepository.findByPlayerStockIdAndSeasonYearAndSeasonTypeOrderByWeekAsc(stockId, 2024, 2))
                .thenReturn(List.of());

        List<PriceHistoryResponse> result = stockQueryService.getPriceHistory(stockId, 2024, 2);

        assertThat(result).isEmpty();
    }

    @Test
    void getPriceHistory_throwsStockNotFoundException_whenStockDoesNotExist() {
        UUID stockId = UUID.randomUUID();

        when(playerStockRepository.existsById(stockId)).thenReturn(false);

        assertThatThrownBy(() -> stockQueryService.getPriceHistory(stockId, 2024, 2))
                .isInstanceOf(StockNotFoundException.class)
                .hasMessageContaining(stockId.toString());
    }

    @Test
    void getPriceHistory_responseFieldsMatchHistoryEntity() {
        UUID stockId = UUID.randomUUID();
        PlayerStock s = stock(stockId, "wr-1", "WR");
        Instant recorded = Instant.parse("2024-09-10T02:15:00Z");
        PriceHistory h = priceHistory(s, 1, new BigDecimal("12.34"));
        ReflectionTestUtils.setField(h, "recordedAt", recorded);

        when(playerStockRepository.existsById(stockId)).thenReturn(true);
        when(priceHistoryRepository.findByPlayerStockIdAndSeasonYearAndSeasonTypeOrderByWeekAsc(stockId, 2024, 2))
                .thenReturn(List.of(h));

        List<PriceHistoryResponse> result = stockQueryService.getPriceHistory(stockId, 2024, 2);

        assertThat(result).hasSize(1);
        PriceHistoryResponse entry = result.get(0);
        assertThat(entry.seasonYear()).isEqualTo(2024);
        assertThat(entry.seasonType()).isEqualTo(2);
        assertThat(entry.week()).isEqualTo(1);
        assertThat(entry.price()).isEqualByComparingTo("12.34");
        assertThat(entry.recordedAt()).isEqualTo(recorded);
    }

    // --- getStock ---

    @Test
    void getStock_returnsStockResponse_whenFound() {
        UUID stockId = UUID.randomUUID();
        PlayerStock s = stock(stockId, "qb-1", "QB");

        when(playerStockRepository.findById(stockId)).thenReturn(Optional.of(s));

        var response = stockQueryService.getStock(stockId);

        assertThat(response.id()).isEqualTo(stockId);
        assertThat(response.athleteEspnId()).isEqualTo("qb-1");
        assertThat(response.position()).isEqualTo("QB");
        assertThat(response.currentPrice()).isEqualByComparingTo("15.00");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void getStock_throwsStockNotFoundException_whenNotFound() {
        UUID stockId = UUID.randomUUID();

        when(playerStockRepository.findById(stockId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockQueryService.getStock(stockId))
                .isInstanceOf(StockNotFoundException.class)
                .hasMessageContaining(stockId.toString());
    }
}
