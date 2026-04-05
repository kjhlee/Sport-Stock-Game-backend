package com.sportstock.ingestion.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
    Map<String, BigDecimal> projectedStats = extractDisplayableStats(playerNode, 1, scoringPeriodId);
    if (projectedStats.isEmpty()) {
      return null;
    }

    ObjectNode result = MAPPER.createObjectNode();
    for (Map.Entry<String, BigDecimal> entry : projectedStats.entrySet()) {
      result.put(entry.getKey(), entry.getValue());
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
    if (isTeamDefense(playerNode)) {
      return null;
    }

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

    if (isKicker(playerNode)) {
      return computeKickerFantasyPoints(stats).total();
    } else {
      return computeOffenseFantasyPoints(stats, statSourceId).total();
    }
  }

  public static Map<String, Object> explainFantasyPoints(
      JsonNode playerNode, int statSourceId, int scoringPeriodId) {
    JsonNode entry = findStatsEntry(playerNode, statSourceId, scoringPeriodId);
    if (entry == null) {
      return Map.of(
          "available", false,
          "statSourceId", statSourceId,
          "scoringPeriodId", scoringPeriodId);
    }

    Map<Integer, BigDecimal> rawStats = extractStats(entry);
    Map<String, BigDecimal> displayableStats =
        extractDisplayableStats(playerNode, statSourceId, scoringPeriodId);
    JsonNode appliedTotalNode = entry.path("appliedTotal");

    if (isTeamDefense(playerNode)) {
      return Map.of(
          "available", false,
          "unsupported", true,
          "reason", "TEAM_DEFENSE fantasy point computation is disabled",
          "statSourceId", statSourceId,
          "scoringPeriodId", scoringPeriodId,
          "normalizedStats", displayableStats);
    }

    boolean usedAppliedTotal = !appliedTotalNode.isMissingNode() && !appliedTotalNode.isNull();
    BigDecimal appliedTotal =
        usedAppliedTotal
            ? BigDecimal.valueOf(appliedTotalNode.asDouble()).setScale(2, RoundingMode.HALF_UP)
            : null;

    FantasyComputation computation =
        isKicker(playerNode)
            ? computeKickerFantasyPoints(rawStats)
            : computeOffenseFantasyPoints(rawStats, statSourceId);

    Map<String, Object> explanation = new LinkedHashMap<>();
    explanation.put("available", true);
    explanation.put("statSourceId", statSourceId);
    explanation.put("scoringPeriodId", scoringPeriodId);
    explanation.put("usedAppliedTotal", usedAppliedTotal);
    explanation.put("appliedTotal", appliedTotal);
    explanation.put("computedFantasyPoints", computation.total());
    explanation.put("normalizedStats", displayableStats);
    explanation.put("scoringBreakdown", computation.breakdown());
    return explanation;
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

  private static FantasyComputation computeOffenseFantasyPoints(
      Map<Integer, BigDecimal> stats, int statSourceId) {
    Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
    addContribution(breakdown, "passingYards", stat(stats, 3), new BigDecimal("0.04"));
    addContribution(breakdown, "passingTouchdowns", stat(stats, 4), new BigDecimal("4"));
    addContribution(breakdown, "passing2PtConversions", stat(stats, 19), new BigDecimal("2"));
    addContribution(
        breakdown, "passingInterceptions", stat(stats, 20), new BigDecimal("-2"));
    addContribution(breakdown, "rushingYards", stat(stats, 24), new BigDecimal("0.1"));
    addContribution(breakdown, "rushingTouchdowns", stat(stats, 25), new BigDecimal("6"));
    addContribution(breakdown, "rushing2PtConversions", stat(stats, 26), new BigDecimal("2"));
    addContribution(
        breakdown, "receptions", offenseReceptions(stats, statSourceId), BigDecimal.ONE);
    addContribution(
        breakdown,
        "receivingYards",
        offenseReceivingYards(stats, statSourceId),
        new BigDecimal("0.1"));
    addContribution(breakdown, "receivingTouchdowns", stat(stats, 43), new BigDecimal("6"));
    addContribution(
        breakdown, "receiving2PtConversions", stat(stats, 44), new BigDecimal("2"));
    addContribution(
        breakdown, "fumbleRecoveredForTD", stat(stats, 63), new BigDecimal("6"));
    addContribution(breakdown, "lostFumbles", stat(stats, 72), new BigDecimal("-2"));
    return new FantasyComputation(sumBreakdown(breakdown), breakdown);
  }

  private static FantasyComputation computeKickerFantasyPoints(Map<Integer, BigDecimal> stats) {
    Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
    addContribution(
        breakdown, "madeFieldGoalsFromUnder40", stat(stats, 80), new BigDecimal("3"));
    addContribution(
        breakdown, "madeFieldGoalsFrom40To49", stat(stats, 77), new BigDecimal("4"));
    addContribution(
        breakdown, "madeFieldGoalsFrom50Plus", stat(stats, 74), new BigDecimal("5"));
    addContribution(breakdown, "madeExtraPoints", stat(stats, 86), BigDecimal.ONE);
    addContribution(breakdown, "missedExtraPoints", stat(stats, 88), new BigDecimal("-1"));
    addContribution(breakdown, "passingYards", stat(stats, 3), new BigDecimal("0.04"));
    addContribution(breakdown, "passingTouchdowns", stat(stats, 4), new BigDecimal("4"));
    addContribution(breakdown, "passing2PtConversions", stat(stats, 19), new BigDecimal("2"));
    addContribution(
        breakdown, "passingInterceptions", stat(stats, 20), new BigDecimal("-2"));
    addContribution(breakdown, "rushingYards", stat(stats, 24), new BigDecimal("0.1"));
    addContribution(breakdown, "rushingTouchdowns", stat(stats, 25), new BigDecimal("6"));
    addContribution(breakdown, "rushing2PtConversions", stat(stats, 26), new BigDecimal("2"));
    addContribution(breakdown, "receptions", stat(stats, 41), BigDecimal.ONE);
    addContribution(breakdown, "receivingYards", stat(stats, 42), new BigDecimal("0.1"));
    addContribution(breakdown, "receivingTouchdowns", stat(stats, 43), new BigDecimal("6"));
    addContribution(
        breakdown, "receiving2PtConversions", stat(stats, 44), new BigDecimal("2"));
    addContribution(
        breakdown, "fumbleRecoveredForTD", stat(stats, 63), new BigDecimal("6"));
    addContribution(breakdown, "lostFumbles", stat(stats, 72), new BigDecimal("-2"));
    return new FantasyComputation(sumBreakdown(breakdown), breakdown);
  }

  private static BigDecimal stat(Map<Integer, BigDecimal> stats, int statId) {
    return stats.getOrDefault(statId, ZERO);
  }

  private static BigDecimal offenseReceptions(Map<Integer, BigDecimal> stats, int statSourceId) {
    if (statSourceId == 1) {
      return firstPresentStat(stats, 53, 41);
    }
    return firstPresentStat(stats, 41, 53);
  }

  private static BigDecimal offenseReceivingTargets(
      Map<Integer, BigDecimal> stats, int statSourceId) {
    if (statSourceId == 1) {
      return firstPresentStat(stats, 58, 53);
    }
    return firstPresentStat(stats, 53, 58);
  }

  private static BigDecimal offenseReceivingYards(
      Map<Integer, BigDecimal> stats, int statSourceId) {
    if (statSourceId == 1) {
      return firstPresentStat(stats, 42, 61);
    }
    return firstPresentStat(stats, 42, 61);
  }

  private static BigDecimal firstPresentStat(Map<Integer, BigDecimal> stats, int... statIds) {
    BigDecimal stat = firstPresentNullableStat(stats, statIds);
    return stat != null ? stat : ZERO;
  }

  private static BigDecimal firstPresentNullableStat(
      Map<Integer, BigDecimal> stats, int... statIds) {
    for (int statId : statIds) {
      if (stats.containsKey(statId)) {
        return stats.get(statId);
      }
    }
    return null;
  }

  private static Map<String, BigDecimal> extractDisplayableStats(
      JsonNode playerNode, int statSourceId, int scoringPeriodId) {
    JsonNode entry = findStatsEntry(playerNode, statSourceId, scoringPeriodId);
    if (entry == null) {
      return Map.of();
    }

    JsonNode statsMap = entry.path("stats");
    if (statsMap.isMissingNode() || !statsMap.isObject()) {
      return Map.of();
    }

    if (isTeamDefense(playerNode)) {
      return Map.of();
    }

    Map<Integer, BigDecimal> stats = extractStats(entry);
    if (stats.isEmpty()) {
      return Map.of();
    }

    if (isKicker(playerNode)) {
      Map<String, BigDecimal> kickerStats = new LinkedHashMap<>();
      putIfPresent(kickerStats, "madeFieldGoalsFromUnder40", firstPresentNullableStat(stats, 80));
      putIfPresent(kickerStats, "madeFieldGoalsFrom40To49", firstPresentNullableStat(stats, 77));
      putIfPresent(kickerStats, "madeFieldGoalsFrom50Plus", firstPresentNullableStat(stats, 74));
      putIfPresent(kickerStats, "madeExtraPoints", firstPresentNullableStat(stats, 86));
      putIfPresent(kickerStats, "missedExtraPoints", firstPresentNullableStat(stats, 88));
      putIfPresent(kickerStats, "passingYards", firstPresentNullableStat(stats, 3));
      putIfPresent(kickerStats, "passingTouchdowns", firstPresentNullableStat(stats, 4));
      putIfPresent(kickerStats, "passing2PtConversions", firstPresentNullableStat(stats, 19));
      putIfPresent(kickerStats, "passingInterceptions", firstPresentNullableStat(stats, 20));
      putIfPresent(kickerStats, "rushingYards", firstPresentNullableStat(stats, 24));
      putIfPresent(kickerStats, "rushingTouchdowns", firstPresentNullableStat(stats, 25));
      putIfPresent(kickerStats, "rushing2PtConversions", firstPresentNullableStat(stats, 26));
      putIfPresent(kickerStats, "receptions", firstPresentNullableStat(stats, 41));
      putIfPresent(kickerStats, "receivingYards", firstPresentNullableStat(stats, 42, 61));
      putIfPresent(kickerStats, "receivingTouchdowns", firstPresentNullableStat(stats, 43));
      putIfPresent(kickerStats, "receiving2PtConversions", firstPresentNullableStat(stats, 44));
      putIfPresent(kickerStats, "fumbleRecoveredForTD", firstPresentNullableStat(stats, 63));
      putIfPresent(kickerStats, "fumbles", firstPresentNullableStat(stats, 68));
      putIfPresent(kickerStats, "lostFumbles", firstPresentNullableStat(stats, 72));
      return kickerStats;
    }

    Map<String, BigDecimal> offenseStats = new LinkedHashMap<>();
    putIfPresent(offenseStats, "passingAttempts", firstPresentNullableStat(stats, 0));
    putIfPresent(offenseStats, "passingCompletions", firstPresentNullableStat(stats, 1));
    putIfPresent(offenseStats, "passingIncompletions", firstPresentNullableStat(stats, 2));
    putIfPresent(offenseStats, "passingYards", firstPresentNullableStat(stats, 3));
    putIfPresent(offenseStats, "passingTouchdowns", firstPresentNullableStat(stats, 4));
    putIfPresent(offenseStats, "passing2PtConversions", firstPresentNullableStat(stats, 19));
    putIfPresent(offenseStats, "passingInterceptions", firstPresentNullableStat(stats, 20));
    putIfPresent(offenseStats, "rushingAttempts", firstPresentNullableStat(stats, 23));
    putIfPresent(offenseStats, "rushingYards", firstPresentNullableStat(stats, 24, 40));
    putIfPresent(offenseStats, "rushingTouchdowns", firstPresentNullableStat(stats, 25));
    putIfPresent(offenseStats, "rushing2PtConversions", firstPresentNullableStat(stats, 26));
    putIfPresent(offenseStats, "receptions", offenseReceptions(stats, statSourceId));
    putIfPresent(
        offenseStats, "receivingYards", offenseReceivingYards(stats, statSourceId));
    putIfPresent(offenseStats, "receivingTouchdowns", firstPresentNullableStat(stats, 43));
    putIfPresent(offenseStats, "receiving2PtConversions", firstPresentNullableStat(stats, 44));
    putIfPresent(
        offenseStats, "receivingTargets", offenseReceivingTargets(stats, statSourceId));
    putIfPresent(offenseStats, "fumbleRecoveredForTD", firstPresentNullableStat(stats, 63));
    putIfPresent(offenseStats, "fumbles", firstPresentNullableStat(stats, 68));
    putIfPresent(offenseStats, "lostFumbles", firstPresentNullableStat(stats, 72));
    return offenseStats;
  }

  private static void putIfPresent(
      Map<String, BigDecimal> result, String name, BigDecimal value) {
    if (value != null) {
      result.put(name, value);
    }
  }

  private static void addContribution(
      Map<String, BigDecimal> breakdown,
      String label,
      BigDecimal value,
      BigDecimal multiplier) {
    breakdown.put(label, value.multiply(multiplier).setScale(2, RoundingMode.HALF_UP));
  }

  private static BigDecimal sumBreakdown(Map<String, BigDecimal> breakdown) {
    BigDecimal total = ZERO;
    for (BigDecimal value : breakdown.values()) {
      total = total.add(value);
    }
    return total.setScale(2, RoundingMode.HALF_UP);
  }

  private record FantasyComputation(BigDecimal total, Map<String, BigDecimal> breakdown) {}
}
