package com.sportstock.scheduler.season;

public enum SeasonPhase {
  OFFSEASON,
  PRESEASON,
  REGULAR_SEASON,
  POSTSEASON;

  public static SeasonPhase fromSeasonType(String seasonType) {
    return switch (seasonType) {
      case "1" -> PRESEASON;
      case "2" -> REGULAR_SEASON;
      case "3" -> POSTSEASON;
      default -> OFFSEASON;
    };
  }

  public boolean supportsLeagueGameplay() {
    return this == REGULAR_SEASON || this == POSTSEASON;
  }

  public boolean supportsDataSync() {
    return this == PRESEASON || this == REGULAR_SEASON || this == POSTSEASON;
  }
}
