package com.sportstock.stockmarket.service;

import com.sportstock.common.dto.stock_market.IngestionInjuryStatusDto;
import com.sportstock.common.enums.stock_market.StockType;
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
    List<String> playerEspnIds = new ArrayList<>();

    for (String teamEspnId : teamEspnIds) {
      List<String> rosterEspnIds =
          ingestionApiClient.getEventRosterEspnIds(eventEspnId, teamEspnId);
      playerEspnIds.addAll(rosterEspnIds);
    }

    int locked = 0;
    if (!teamEspnIds.isEmpty()) {
      locked += stockRepository.lockByEspnIdsAndType(teamEspnIds, StockType.TEAM_DEFENSE);
    }
    if (!playerEspnIds.isEmpty()) {
      locked += stockRepository.lockByEspnIdsAndType(playerEspnIds, StockType.PLAYER);
    }

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
  public InjurySyncResult syncInjuryStatuses(int seasonYear) {
    log.info("Syncing injury statuses for season {}", seasonYear);

    List<String> injuredEspnIds =
        ingestionApiClient.getInjuredAthletes(seasonYear).stream()
            .map(IngestionInjuryStatusDto::athleteEspnId)
            .distinct()
            .toList();

    int unlocked = stockRepository.clearAllPlayerInjuryLocks();
    int locked =
        injuredEspnIds.isEmpty() ? 0 : stockRepository.setInjuryLockedByEspnIds(injuredEspnIds);

    return new InjurySyncResult(locked, unlocked, 0);
  }

  public record InjurySyncResult(int locked, int unlocked, int unchanged) {}
}
