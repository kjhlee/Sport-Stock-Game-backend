package com.sportstock.ingestion.dto.response;

import com.sportstock.ingestion.entity.TeamRecord;

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
        BigDecimal leagueWinPercent
) {
    public static TeamRecordResponse from(TeamRecord entity) {
        return new TeamRecordResponse(
                entity.getTeam().getEspnId(),
                entity.getSeasonYear(),
                entity.getRecordType(),
                entity.getSummary(),
                entity.getWins(),
                entity.getLosses(),
                entity.getTies(),
                entity.getWinPercent(),
                entity.getOtWins(),
                entity.getOtLosses(),
                entity.getPointsFor(),
                entity.getPointsAgainst(),
                entity.getPointDifferential(),
                entity.getAvgPointsFor(),
                entity.getAvgPointsAgainst(),
                entity.getPlayoffSeed(),
                entity.getStreak(),
                entity.getGamesPlayed(),
                entity.getDivisionWins(),
                entity.getDivisionLosses(),
                entity.getDivisionTies(),
                entity.getLeagueWinPercent()
        );
    }
}
