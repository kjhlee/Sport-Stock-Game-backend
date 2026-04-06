package com.sportstock.ingestion.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportstock.ingestion.entity.PlayerGameStat;
import com.sportstock.ingestion.repo.PlayerGameStatRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlayerGameStatsFantasyPointCalculator {

  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

  private final PlayerGameStatRepository playerGameStatRepository;
  private final ObjectMapper objectMapper;

  public BigDecimal computePlayerFantasyPoints(Long eventId, String athleteEspnId) {
    List<PlayerGameStat> rows =
        playerGameStatRepository.findByEventIdAndAthleteEspnId(eventId, athleteEspnId);
    if (rows.isEmpty()) {
      return null;
    }

    Map<String, String> passing = category(rows, "passing");
    Map<String, String> rushing = category(rows, "rushing");
    Map<String, String> receiving = category(rows, "receiving");
    Map<String, String> fumbles = category(rows, "fumbles");

    if (passing.isEmpty() && rushing.isEmpty() && receiving.isEmpty() && fumbles.isEmpty()) {
      return null;
    }

    BigDecimal total = ZERO;

    total = total.add(num(passing, "passingYards").multiply(new BigDecimal("0.04")));
    total = total.add(num(passing, "passingTouchdowns").multiply(new BigDecimal("4")));
    total = total.add(num(passing, "interceptions").multiply(new BigDecimal("-2")));

    total = total.add(num(rushing, "rushingYards").multiply(new BigDecimal("0.1")));
    total = total.add(num(rushing, "rushingTouchdowns").multiply(new BigDecimal("6")));

    total = total.add(num(receiving, "receptions"));
    total = total.add(num(receiving, "receivingYards").multiply(new BigDecimal("0.1")));
    total = total.add(num(receiving, "receivingTouchdowns").multiply(new BigDecimal("6")));

    total = total.add(num(fumbles, "fumblesLost").multiply(new BigDecimal("-2")));

    return total.setScale(2, RoundingMode.HALF_UP);
  }

  private Map<String, String> category(List<PlayerGameStat> rows, String name) {
    for (PlayerGameStat row : rows) {
      if (!name.equalsIgnoreCase(row.getStatCategory())) {
        continue;
      }
      try {
        return objectMapper.readValue(row.getStats(), new TypeReference<>() {});
      } catch (Exception e) {
        log.warn(
            "Failed to parse stats JSON for athlete {} event {} category {}",
            row.getAthleteEspnId(),
            row.getEvent().getId(),
            row.getStatCategory(),
            e);
        return Map.of();
      }
    }
    return Map.of();
  }

  private BigDecimal num(Map<String, String> stats, String key) {
    String raw = stats.get(key);
    if (raw == null || raw.isBlank() || "--".equals(raw)) {
      return ZERO;
    }
    try {
      return new BigDecimal(raw.replace(",", ""));
    } catch (NumberFormatException e) {
      return ZERO;
    }
  }
}
