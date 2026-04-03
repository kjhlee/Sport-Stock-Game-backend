package com.sportstock.ingestion.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FantasySnapshotMapper {

  private FantasySnapshotMapper() {}

  private static final int DST_POSITION_ID = 16;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static boolean isTeamDefense(JsonNode playerNode) {
    return playerNode.path("defaultPositionId").asInt(-1) == DST_POSITION_ID;
  }

  public static String extractProTeamId(JsonNode playerNode) {
    return String.valueOf(playerNode.path("proTeamId").asInt());
  }

  public static String extractPlayerId(JsonNode playerNode) {
    return String.valueOf(playerNode.path("id").asInt());
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
    if (appliedTotal.isMissingNode() || appliedTotal.isNull()) {
      return null;
    }
    return BigDecimal.valueOf(appliedTotal.asDouble()).setScale(2, BigDecimal.ROUND_HALF_UP);
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
}
