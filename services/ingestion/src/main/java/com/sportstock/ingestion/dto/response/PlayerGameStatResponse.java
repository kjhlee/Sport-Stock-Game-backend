package com.sportstock.ingestion.dto.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportstock.ingestion.entity.PlayerGameStat;
import com.sportstock.ingestion.exception.IngestionException;

import java.util.Map;

public record PlayerGameStatResponse(
        String eventEspnId,
        String athleteEspnId,
        String teamEspnId,
        String statCategory,
        Map<String, String> stats
) {
    public static PlayerGameStatResponse from(PlayerGameStat entity, ObjectMapper objectMapper) {
        Map<String, String> statsMap;
        try {
            statsMap = objectMapper.readValue(entity.getStats(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new IngestionException("Failed to parse player stats JSON", e);
        }
        return new PlayerGameStatResponse(
                entity.getEvent().getEspnId(),
                entity.getAthleteEspnId(),
                entity.getTeam().getEspnId(),
                entity.getStatCategory(),
                statsMap
        );
    }
}
