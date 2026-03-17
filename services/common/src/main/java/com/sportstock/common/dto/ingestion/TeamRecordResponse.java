package com.sportstock.common.dto.ingestion;

import java.math.BigDecimal;

public record TeamRecordResponse(
    String teamEspnId,
    Integer seasonYear,
    String recordType,
    String summary,
    Integer wins,
    Integer losses,
    Integer ties,
    BigDecimal winPercent,
    Integer otWins,
    Integer otLosses,
    Integer pointsFor,
    Integer pointsAgainst,
    Integer pointDifferential,
    BigDecimal avgPointsFor,
    BigDecimal avgPointsAgainst,
    Integer playoffSeed,
    Integer streak,
    Integer gamesPlayed,
    Integer divisionWins,
    Integer divisionLosses,
    Integer divisionTies,
    BigDecimal leagueWinPercent) {}
