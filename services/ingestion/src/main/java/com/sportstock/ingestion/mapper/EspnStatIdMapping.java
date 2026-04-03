package com.sportstock.ingestion.mapper;

import java.util.Map;

public final class EspnStatIdMapping {
    private EspnStatIdMapping() {}

    public static final Map<Integer, String> STAT_NAMES =
            Map.ofEntries(
                    Map.entry(0, "passingAttempts"),
                    Map.entry(1, "passingCompletions"),
                    Map.entry(3, "passingYards"),
                    Map.entry(4, "passingTouchdowns"),
                    Map.entry(20, "interceptions"),
                    Map.entry(23, "rushingAttempts"),
                    Map.entry(24, "rushingYards"),
                    Map.entry(25, "rushingTouchdowns"),
                    Map.entry(41, "receptions"),
                    Map.entry(42, "receivingYards"),
                    Map.entry(43, "receivingTouchdowns"),
                    Map.entry(53, "receivingTargets"),
                    Map.entry(68, "fumbles"),
                    Map.entry(72, "lostFumbles"),
                    Map.entry(74, "madeFieldGoals"),
                    Map.entry(77, "missedFieldGoals"),
                    Map.entry(79, "madeExtraPoints"),
                    Map.entry(80, "missedExtraPoints"),
                    Map.entry(83, "defSacks"),
                    Map.entry(85, "defInterceptions"),
                    Map.entry(86, "defFumblesRecovered"),
                    Map.entry(88, "defBlockedKicks"),
                    Map.entry(89, "defSafeties"),
                    Map.entry(90, "defTouchdowns"),
                    Map.entry(95, "defPointsAllowed"),
                    Map.entry(99, "defYardsAllowed"));

    public static String resolve(int statId) {
        return STAT_NAMES.getOrDefault(statId, "stat_" + statId);
    }
}
