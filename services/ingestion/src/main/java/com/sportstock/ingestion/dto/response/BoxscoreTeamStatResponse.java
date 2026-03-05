package com.sportstock.ingestion.dto.response;

import com.sportstock.ingestion.entity.BoxscoreTeamStat;

public record BoxscoreTeamStatResponse(
        String eventEspnId,
        String teamEspnId,
        String homeAway,
        String statName,
        String statValue,
        String displayValue,
        String label
) {
    public static BoxscoreTeamStatResponse from(BoxscoreTeamStat entity) {
        return new BoxscoreTeamStatResponse(
                entity.getEvent().getEspnId(),
                entity.getTeam().getEspnId(),
                entity.getHomeAway(),
                entity.getStatName(),
                entity.getStatValue(),
                entity.getDisplayValue(),
                entity.getLabel()
        );
    }
}
