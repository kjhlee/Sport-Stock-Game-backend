package com.sportstocks.stockmarket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.sportstocks.stockmarket.client.IngestionApiClient;
import com.sportstocks.stockmarket.config.PricingConfig;
import com.sportstocks.stockmarket.dto.IngestionEventDto;
import com.sportstocks.stockmarket.dto.IngestionPlayerGameStatsDto;
import com.sportstocks.stockmarket.model.entity.PlayerStock;
import com.sportstocks.stockmarket.model.entity.PriceHistory;
import com.sportstocks.stockmarket.model.enums.StockStatus;
import com.sportstocks.stockmarket.repository.PlayerStockRepository;
import com.sportstocks.stockmarket.repository.PriceHistoryRepository;
import com.sportstocks.stockmarket.service.PricingService.PriceUpdateResult;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PricingServiceTest {

    @Mock private IngestionApiClient ingestionApiClient;
    @Mock private PlayerStockRepository playerStockRepository;
    @Mock private PriceHistoryRepository priceHistoryRepository;
    @Mock private PricingConfig pricingConfig;

    @InjectMocks private PricingService pricingService;

    @BeforeEach
    void setUp() {
        when(pricingConfig.getSmoothingAlpha()).thenReturn(new BigDecimal("0.6"));
        when(pricingConfig.getPriceFloor()).thenReturn(new BigDecimal("1.00"));
    }

    // --- helpers ---

    private IngestionEventDto completedEvent(String espnId) {
        IngestionEventDto event = new IngestionEventDto();
        ReflectionTestUtils.setField(event, "espnId", espnId);
        ReflectionTestUtils.setField(event, "statusCompleted", true);
        return event;
    }

    private IngestionPlayerGameStatsDto stat(String athleteId, String category, Map<String, String> stats) {
        IngestionPlayerGameStatsDto dto = new IngestionPlayerGameStatsDto();
        ReflectionTestUtils.setField(dto, "athleteEspnId", athleteId);
        ReflectionTestUtils.setField(dto, "statCategory", category);
        ReflectionTestUtils.setField(dto, "stats", stats);
        return dto;
    }

    private PlayerStock stock(String athleteId, String position, BigDecimal price) {
        PlayerStock s = new PlayerStock();
        ReflectionTestUtils.setField(s, "id", UUID.randomUUID());
        s.setAthleteEspnId(athleteId);
        s.setFullName("Test Player");
        s.setPosition(position);
        s.setCurrentPrice(price);
        s.setStatus(StockStatus.ACTIVE);
        return s;
    }

    private void stubEventAndStats(String eventId, List<IngestionPlayerGameStatsDto> stats) {
        when(ingestionApiClient.getEvents(2024, 2, 1)).thenReturn(List.of(completedEvent(eventId)));
        when(ingestionApiClient.getPlayerStats(eventId)).thenReturn(stats);
        when(priceHistoryRepository.findByPlayerStockIdAndSeasonYearAndSeasonTypeAndWeek(any(), eq(2024), eq(2), eq(1)))
                .thenReturn(Optional.empty());
        when(priceHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // --- no events ---

    @Test
    void updatePricesForWeek_noEvents_returnsZeroCounts() {
        when(ingestionApiClient.getEvents(2024, 2, 1)).thenReturn(List.of());

        PriceUpdateResult result = pricingService.updatePricesForWeek(2024, 2, 1);

        assertThat(result.updated()).isZero();
        assertThat(result.skipped()).isZero();
        verify(priceHistoryRepository, never()).save(any());
    }

    @Test
    void updatePricesForWeek_noCompletedEvents_returnsZeroCounts() {
        IngestionEventDto incomplete = new IngestionEventDto();
        ReflectionTestUtils.setField(incomplete, "espnId", "evt-1");
        ReflectionTestUtils.setField(incomplete, "statusCompleted", false);
        when(ingestionApiClient.getEvents(2024, 2, 1)).thenReturn(List.of(incomplete));

        PriceUpdateResult result = pricingService.updatePricesForWeek(2024, 2, 1);

        assertThat(result.updated()).isZero();
        assertThat(result.skipped()).isZero();
        verify(priceHistoryRepository, never()).save(any());
    }

    // --- skipped athlete ---

    @Test
    void updatePricesForWeek_athleteWithNoStock_countsAsSkipped() {
        when(ingestionApiClient.getEvents(2024, 2, 1)).thenReturn(List.of(completedEvent("evt-1")));
        when(ingestionApiClient.getPlayerStats("evt-1")).thenReturn(
                List.of(stat("athlete-99", "passing", Map.of("passingYards", "300"))));
        when(playerStockRepository.findByAthleteEspnId("athlete-99")).thenReturn(Optional.empty());

        PriceUpdateResult result = pricingService.updatePricesForWeek(2024, 2, 1);

        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.updated()).isZero();
        verify(priceHistoryRepository, never()).save(any());
    }

    // --- QB scoring ---
    // 300 pass yds / 25 = 12.0, 3 pass TDs × 4 = 12.0, 1 INT × 2 = -2.0 → passing = 22.0
    // 30 rush yds / 10 = 3.0, 1 rush TD × 6 = 6.0 → rushing = 9.0
    // total = 31.0 → newPrice = 0.6 × 31.0 + 0.4 × 15.0 = 24.60

    @Test
    void updatePricesForWeek_qbStats_savesCorrectPrice() {
        PlayerStock qb = stock("qb-1", "QB", new BigDecimal("15.00"));
        stubEventAndStats("evt-1", List.of(
                stat("qb-1", "passing", Map.of("passingYards", "300", "passingTouchdowns", "3", "interceptions", "1")),
                stat("qb-1", "rushing", Map.of("rushingYards", "30", "rushingTouchdowns", "1"))
        ));
        when(playerStockRepository.findByAthleteEspnId("qb-1")).thenReturn(Optional.of(qb));

        PriceUpdateResult result = pricingService.updatePricesForWeek(2024, 2, 1);

        assertThat(result.updated()).isEqualTo(1);
        ArgumentCaptor<PriceHistory> captor = forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo("24.60");
        assertThat(qb.getCurrentPrice()).isEqualByComparingTo("24.60");
    }

    // --- RB scoring ---
    // 100 rush yds / 10 = 10.0, 1 rush TD × 6 = 6.0
    // 5 receptions = 5.0, 50 recv yds / 10 = 5.0, 1 recv TD × 6 = 6.0
    // total = 32.0 → newPrice = 0.6 × 32.0 + 0.4 × 12.0 = 24.00

    @Test
    void updatePricesForWeek_rbStats_savesCorrectPrice() {
        PlayerStock rb = stock("rb-1", "RB", new BigDecimal("12.00"));
        stubEventAndStats("evt-1", List.of(
                stat("rb-1", "rushing", Map.of("rushingYards", "100", "rushingTouchdowns", "1")),
                stat("rb-1", "receiving", Map.of("receptions", "5", "receivingYards", "50", "receivingTouchdowns", "1"))
        ));
        when(playerStockRepository.findByAthleteEspnId("rb-1")).thenReturn(Optional.of(rb));

        PriceUpdateResult result = pricingService.updatePricesForWeek(2024, 2, 1);

        assertThat(result.updated()).isEqualTo(1);
        ArgumentCaptor<PriceHistory> captor = forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo("24.00");
    }

    // --- WR scoring ---
    // 8 receptions = 8.0, 120 recv yds / 10 = 12.0, 2 recv TDs × 6 = 12.0
    // total = 32.0 → newPrice = 0.6 × 32.0 + 0.4 × 10.0 = 23.20

    @Test
    void updatePricesForWeek_wrStats_savesCorrectPrice() {
        PlayerStock wr = stock("wr-1", "WR", new BigDecimal("10.00"));
        stubEventAndStats("evt-1", List.of(
                stat("wr-1", "receiving", Map.of("receptions", "8", "receivingYards", "120", "receivingTouchdowns", "2"))
        ));
        when(playerStockRepository.findByAthleteEspnId("wr-1")).thenReturn(Optional.of(wr));

        PriceUpdateResult result = pricingService.updatePricesForWeek(2024, 2, 1);

        assertThat(result.updated()).isEqualTo(1);
        ArgumentCaptor<PriceHistory> captor = forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo("23.20");
    }

    // --- TE scoring (same formula as WR) ---
    // 4 receptions = 4.0, 60 recv yds / 10 = 6.0, 1 recv TD × 6 = 6.0
    // total = 16.0 → newPrice = 0.6 × 16.0 + 0.4 × 8.0 = 12.80

    @Test
    void updatePricesForWeek_teStats_savesCorrectPrice() {
        PlayerStock te = stock("te-1", "TE", new BigDecimal("8.00"));
        stubEventAndStats("evt-1", List.of(
                stat("te-1", "receiving", Map.of("receptions", "4", "receivingYards", "60", "receivingTouchdowns", "1"))
        ));
        when(playerStockRepository.findByAthleteEspnId("te-1")).thenReturn(Optional.of(te));

        pricingService.updatePricesForWeek(2024, 2, 1);

        ArgumentCaptor<PriceHistory> captor = forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo("12.80");
    }

    // --- K scoring ---
    // FG made=3, FG attempted=4, FG missed=1, XP made=3
    // score = 3×3 + 3 - 1 = 11.0 → newPrice = 0.6 × 11.0 + 0.4 × 5.0 = 8.60

    @Test
    void updatePricesForWeek_kickerStats_savesCorrectPrice() {
        PlayerStock k = stock("k-1", "K", new BigDecimal("5.00"));
        stubEventAndStats("evt-1", List.of(
                stat("k-1", "kicking", Map.of(
                        "fieldGoalsMade/fieldGoalsAttempted", "3/4",
                        "extraPointsMade/extraPointsAttempted", "3/3"
                ))
        ));
        when(playerStockRepository.findByAthleteEspnId("k-1")).thenReturn(Optional.of(k));

        pricingService.updatePricesForWeek(2024, 2, 1);

        ArgumentCaptor<PriceHistory> captor = forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo("8.60");
    }

    // --- price floor ---
    // Zero stats → performance score = 0, newPrice = 0.4 × 1.50 = 0.60 → clamped to 1.00

    @Test
    void updatePricesForWeek_priceFloorApplied_whenNewPriceBelowFloor() {
        PlayerStock wr = stock("wr-bad", "WR", new BigDecimal("1.50"));
        stubEventAndStats("evt-1", List.of(
                stat("wr-bad", "receiving", Map.of("receptions", "0", "receivingYards", "0", "receivingTouchdowns", "0"))
        ));
        when(playerStockRepository.findByAthleteEspnId("wr-bad")).thenReturn(Optional.of(wr));

        pricingService.updatePricesForWeek(2024, 2, 1);

        ArgumentCaptor<PriceHistory> captor = forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo("1.00");
    }

    // --- upsert existing PriceHistory ---
    // 250 pass yds / 25 = 10.0, 2 TDs × 4 = 8.0 → score = 18.0
    // newPrice = 0.6 × 18.0 + 0.4 × 20.0 = 18.80

    @Test
    void updatePricesForWeek_updatesExistingPriceHistoryRecord() {
        PlayerStock qb = stock("qb-2", "QB", new BigDecimal("20.00"));
        PriceHistory existing = new PriceHistory();
        existing.setPlayerStock(qb);
        existing.setSeasonYear(2024);
        existing.setSeasonType(2);
        existing.setWeek(1);
        existing.setPrice(new BigDecimal("18.00"));

        when(ingestionApiClient.getEvents(2024, 2, 1)).thenReturn(List.of(completedEvent("evt-1")));
        when(ingestionApiClient.getPlayerStats("evt-1")).thenReturn(List.of(
                stat("qb-2", "passing", Map.of("passingYards", "250", "passingTouchdowns", "2", "interceptions", "0"))
        ));
        when(playerStockRepository.findByAthleteEspnId("qb-2")).thenReturn(Optional.of(qb));
        when(priceHistoryRepository.findByPlayerStockIdAndSeasonYearAndSeasonTypeAndWeek(any(), eq(2024), eq(2), eq(1)))
                .thenReturn(Optional.of(existing));
        when(priceHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        pricingService.updatePricesForWeek(2024, 2, 1);

        ArgumentCaptor<PriceHistory> captor = forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        assertThat(captor.getValue().getPrice()).isEqualByComparingTo("18.80");
    }

    // --- multiple athletes in one week ---

    @Test
    void updatePricesForWeek_multipleAthletes_countsAllUpdates() {
        PlayerStock qb = stock("qb-1", "QB", new BigDecimal("15.00"));
        PlayerStock wr = stock("wr-1", "WR", new BigDecimal("10.00"));

        when(ingestionApiClient.getEvents(2024, 2, 1)).thenReturn(List.of(completedEvent("evt-1")));
        when(ingestionApiClient.getPlayerStats("evt-1")).thenReturn(List.of(
                stat("qb-1", "passing", Map.of("passingYards", "200", "passingTouchdowns", "1", "interceptions", "0")),
                stat("wr-1", "receiving", Map.of("receptions", "5", "receivingYards", "80", "receivingTouchdowns", "1"))
        ));
        when(playerStockRepository.findByAthleteEspnId("qb-1")).thenReturn(Optional.of(qb));
        when(playerStockRepository.findByAthleteEspnId("wr-1")).thenReturn(Optional.of(wr));
        when(priceHistoryRepository.findByPlayerStockIdAndSeasonYearAndSeasonTypeAndWeek(any(), eq(2024), eq(2), eq(1)))
                .thenReturn(Optional.empty());
        when(priceHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PriceUpdateResult result = pricingService.updatePricesForWeek(2024, 2, 1);

        assertThat(result.updated()).isEqualTo(2);
        assertThat(result.skipped()).isZero();
    }
}
