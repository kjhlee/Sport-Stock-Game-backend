package com.sportstocks.stockmarket.client.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IngestionPlayerGameStatsDto {
    private String eventEspnId;
    private String athleteEspnId;
    private String teamEspnId;
    private String statCategory;
    private Map<String, String> stats;
}