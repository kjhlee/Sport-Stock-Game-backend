package com.sportstock.ingestion.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FantasySnapshotMapper {

  private FantasySnapshotMapper() {}

  private static final int DST_POSITION_ID = 16;
  private static final int KICKER_POSITION_ID = 5;
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

  public static boolean isTeamDefense(JsonNode playerNode) {
    return playerNode.path("defaultPositionId").asInt(-1) == DST_POSITION_ID;
  }

  public static boolean isKicker(JsonNode playerNode) {
    return playerNode.path("defaultPositionId").asInt(-1) == KICKER_POSITION_ID;
  }

  public static String extractProTeamId(JsonNode playerNode) {
    int proTeamId = playerNode.path("proTeamId").asInt(-1);
    return proTeamId > 0 ? String.valueOf(proTeamId) : null;
  }

  public static String extractPlayerId(JsonNode playerNode) {
    int playerId = playerNode.path("id").asInt(-1);
    return playerId > 0 ? String.valueOf(playerId) : null;
  }

  public static String extractFullName(JsonNode playerNode) {
    return playerNode.path("fullName").asText("Unknown");
  }

  public static BigDecimal extractProjectedFantasyPoints(JsonNode playerNode, int scoringPeriodId) {
    return extractAppliedTotal(playerNode, 1, scoringPeriodId);
  }

  public static BigDecimal extractActualFantasyPoints(JsonNode playerNode, int scoringPeriodId) {
    return extractAppliedTotal(playerNode, 0, scoringPeriodId);
  }

  public static String extractProjectedStatsJson(JsonNode playerNode, int scoringPeriodId) {
    JsonNode statsEntry = findStatsEntry(playerNode, 1, scoringPeriodId);
    if (statsEntry == null) {
      return null;
    }

    JsonNode statsMap = statsEntry.path("stats");
    if (statsMap.isMissingNode() || !statsMap.isObject()) {
      return null;
    }

    ObjectNode result = MAPPER.createObjectNode();
    Iterator<Map.Entry<String, JsonNode>> fields = statsMap.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      try {
        int statId = Integer.parseInt(field.getKey());
        String readableName = EspnStatIdMapping.resolve(statId);
        result.put(readableName, field.getValue().asDouble());
      } catch (NumberFormatException e) {
        log.debug("Skipping non-numeric stat key: {}", field.getKey());
      }
    }

    try {
      return MAPPER.writeValueAsString(result);
    } catch (Exception e) {
      log.error("Failed to serialize projected stats to JSON", e);
      return null;
    }
  }

  private static BigDecimal extractAppliedTotal(
      JsonNode playerNode, int statSourceId, int scoringPeriodId) {
    JsonNode entry = findStatsEntry(playerNode, statSourceId, scoringPeriodId);
    if (entry == null) {
      return null;
    }
    JsonNode appliedTotal = entry.path("appliedTotal");
    if (!appliedTotal.isMissingNode() && !appliedTotal.isNull()) {
      return BigDecimal.valueOf(appliedTotal.asDouble()).setScale(2, RoundingMode.HALF_UP);
    }

    Map<Integer, BigDecimal> stats = extractStats(entry);
    if (stats.isEmpty()) {
      return null;
    }

    if (isTeamDefense(playerNode)) {
      return computeDefenseFantasyPoints(stats);
    } else if (isKicker(playerNode)) {
      return computeKickerFantasyPoints(stats);
    } else {
      return computeOffenseFantasyPoints(stats);
    }
  }

  private static JsonNode findStatsEntry(
      JsonNode playerNode, int statSourceId, int scoringPeriodId) {
    JsonNode statsArray = playerNode.path("stats");
    if (!statsArray.isArray()) {
      return null;
    }
    for (JsonNode entry : statsArray) {
      if (entry.path("statSourceId").asInt(-1) == statSourceId
          && entry.path("scoringPeriodId").asInt(-1) == scoringPeriodId) {
        return entry;
      }
    }
    return null;
  }

  private static Map<Integer, BigDecimal> extractStats(JsonNode entry) {
    JsonNode statsNode = entry.path("stats");
    if (!statsNode.isObject()) {
      return Map.of();
    }

    Map<Integer, BigDecimal> stats = new HashMap<>();
    Iterator<Map.Entry<String, JsonNode>> fields = statsNode.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      try {
        stats.put(Integer.parseInt(field.getKey()), BigDecimal.valueOf(field.getValue().asDouble()));
      } catch (NumberFormatException e) {
        log.debug("Skipping non-numeric fantasy stat key: {}", field.getKey());
      }
    }
    return stats;
  }

  private static BigDecimal computeOffenseFantasyPoints(Map<Integer, BigDecimal> stats) {
    BigDecimal total = ZERO;
    // Passing
    total = total.add(stat(stats, 3).multiply(new BigDecimal("0.04")));
    total = total.add(stat(stats, 4).multiply(new BigDecimal("4")));
    total = total.add(stat(stats, 19).multiply(new BigDecimal("2")));
    total = total.subtract(stat(stats, 20).multiply(new BigDecimal("2")));
    // Rushing
    total = total.add(stat(stats, 24).multiply(new BigDecimal("0.1")));
    total = total.add(stat(stats, 25).multiply(new BigDecimal("6")));
    total = total.add(stat(stats, 26).multiply(new BigDecimal("2")));
    // Receiving (PPR)
    total = total.add(stat(stats, 41));
    total = total.add(stat(stats, 42).multiply(new BigDecimal("0.1")));
    total = total.add(stat(stats, 43).multiply(new BigDecimal("6")));
    total = total.add(stat(stats, 44).multiply(new BigDecimal("2")));
    // Misc
    total = total.add(stat(stats, 63).multiply(new BigDecimal("6")));
    total = total.subtract(stat(stats, 72).multiply(new BigDecimal("2")));
    return total.setScale(2, RoundingMode.HALF_UP);
  }

  private static BigDecimal computeKickerFantasyPoints(Map<Integer, BigDecimal> stats) {
    BigDecimal total = ZERO;
    // Kicking
    total = total.add(stat(stats, 80).multiply(new BigDecimal("3")));
    total = total.add(stat(stats, 77).multiply(new BigDecimal("4")));
    total = total.add(stat(stats, 74).multiply(new BigDecimal("5")));
    total = total.add(stat(stats, 86));
    total = total.subtract(stat(stats, 88));
    // Non-kicking plays (trick plays, fumble recoveries, etc.)
    total = total.add(stat(stats, 3).multiply(new BigDecimal("0.04")));
    total = total.add(stat(stats, 4).multiply(new BigDecimal("4")));
    total = total.add(stat(stats, 19).multiply(new BigDecimal("2")));
    total = total.subtract(stat(stats, 20).multiply(new BigDecimal("2")));
    total = total.add(stat(stats, 24).multiply(new BigDecimal("0.1")));
    total = total.add(stat(stats, 25).multiply(new BigDecimal("6")));
    total = total.add(stat(stats, 26).multiply(new BigDecimal("2")));
    total = total.add(stat(stats, 41));
    total = total.add(stat(stats, 42).multiply(new BigDecimal("0.1")));
    total = total.add(stat(stats, 43).multiply(new BigDecimal("6")));
    total = total.add(stat(stats, 44).multiply(new BigDecimal("2")));
    total = total.add(stat(stats, 63).multiply(new BigDecimal("6")));
    total = total.subtract(stat(stats, 72).multiply(new BigDecimal("2")));
    return total.setScale(2, RoundingMode.HALF_UP);
  }

  private static BigDecimal computeDefenseFantasyPoints(Map<Integer, BigDecimal> stats) {
    BigDecimal total = ZERO;
    total = total.add(stat(stats, 99));
    total = total.add(stat(stats, 95).multiply(new BigDecimal("2")));
    total = total.add(stat(stats, 96).multiply(new BigDecimal("2")));
    total = total.add(stat(stats, 97).multiply(new BigDecimal("2")));
    total = total.add(stat(stats, 98).multiply(new BigDecimal("2")));
    total = total.add(stat(stats, 105).multiply(new BigDecimal("6")));
    total = total.add(pointsAllowedBonus(stat(stats, 120)));
    return total.setScale(2, RoundingMode.HALF_UP);
  }

  private static BigDecimal pointsAllowedBonus(BigDecimal pointsAllowed) {
    int pa = pointsAllowed.setScale(0, RoundingMode.DOWN).intValue();
    if (pa == 0) {
      return new BigDecimal("10");
    }
    if (pa <= 6) {
      return new BigDecimal("7");
    }
    if (pa <= 13) {
      return new BigDecimal("4");
    }
    if (pa <= 17) {
      return BigDecimal.ONE;
    }
    if (pa <= 21) {
      return ZERO;
    }
    if (pa <= 27) {
      return new BigDecimal("-1");
    }
    if (pa <= 34) {
      return new BigDecimal("-4");
    }
    if (pa <= 45) {
      return new BigDecimal("-7");
    }
    return new BigDecimal("-7");
  }

  private static BigDecimal stat(Map<Integer, BigDecimal> stats, int statId) {
    return stats.getOrDefault(statId, ZERO);
  }
}
