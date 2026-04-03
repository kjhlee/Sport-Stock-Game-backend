package com.sportstock.stockmarket.service;

import com.sportstock.common.dto.stock_market.IngestionEventDto;
import com.sportstock.stockmarket.client.IngestionApiClient;
import com.sportstock.stockmarket.repository.StockRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockLockService {

    private final StockRepository stockRepository;
    private final IngestionApiClient ingestionApiClient;

    @Transactional
    public int lockPlayersForEvent(String eventEspnId) {
        log.info("Locking players for event {}", eventEspnId);

        List<String> teamEspnIds = ingestionApiClient.getEventTeamEspnIds(eventEspnId);
        List<String> allEspnIds = new ArrayList<>(teamEspnIds);

        for (String teamEspnId : teamEspnIds) {
            List<String> rosterEspnIds = ingestionApiClient.getEventRosterEspnIds(eventEspnId, teamEspnId);
            allEspnIds.addAll(rosterEspnIds);
        }

        int locked = stockRepository.lockByEspnIds(allEspnIds);
        log.info("Locked {} stocks for event {}", locked, eventEspnId);
        return locked;
    }

    @Transactional
    public int unlockAllForWeek() {
        int unlocked = stockRepository.unlockAllGameLocks();
        log.info("Unlocked {} game-locked stocks", unlocked);
        return unlocked;
    }

    @Transactional
    public InjurySyncResult syncInjuryStatuses() {
        // TODO: implement full injury sync
        log.info("Syncing injury statuses -- stub implementation");
        // Full implementation depends on ingestion exposing injury data endpoint
        return new InjurySyncResult(0, 0, 0);
    }

    public record InjurySyncResult(int locked, int unlocked, int unchanged) {}
}