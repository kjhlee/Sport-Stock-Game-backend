package com.sportstock.ingestion.mapper;

import java.util.Map;

public final class EspnStatIdMapping {
  private EspnStatIdMapping() {}

  public static final Map<Integer, String> STAT_NAMES =
      Map.ofEntries(
          // Passing
          Map.entry(0, "passingAttempts"),
          Map.entry(1, "passingCompletions"),
          Map.entry(2, "passingIncompletions"),
          Map.entry(3, "passingYards"),
          Map.entry(4, "passingTouchdowns"),
          Map.entry(19, "passing2PtConversions"),
          Map.entry(20, "passingInterceptions"),
          // Rushing
          Map.entry(23, "rushingAttempts"),
          Map.entry(24, "rushingYards"),
          Map.entry(25, "rushingTouchdowns"),
          Map.entry(26, "rushing2PtConversions"),
          // Receiving
          Map.entry(41, "receptions"),
          Map.entry(42, "receivingYards"),
          Map.entry(43, "receivingTouchdowns"),
          Map.entry(44, "receiving2PtConversions"),
          Map.entry(53, "receivingTargets"),
          // Fumbles
          Map.entry(63, "fumbleRecoveredForTD"),
          Map.entry(68, "fumbles"),
          Map.entry(72, "lostFumbles"),
          // Defense / Special Teams
          Map.entry(95, "defensiveInterceptions"),
          Map.entry(96, "defensiveFumblesRecovered"),
          Map.entry(97, "defensiveBlockedKicks"),
          Map.entry(98, "defensiveSafeties"),
          Map.entry(99, "defensiveSacks"),
          Map.entry(105, "defensivePlusSpecialTeamsTouchdowns"),
          Map.entry(120, "defensivePointsAllowed"));

  public static String resolve(int statId) {
    return STAT_NAMES.getOrDefault(statId, "stat_" + statId);
  }
}
