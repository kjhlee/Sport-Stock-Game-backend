package com.sportstock.common.dto.ingestion;

public record BoxscoreTeamStatResponse(
        String eventEspnId,
        String teamEspnId,
        String homeAway,
        String statName,
        String statValue,
        String displayValue,
        String label
) {
}
