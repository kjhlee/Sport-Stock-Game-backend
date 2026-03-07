package com.sportstocks.stockmarket.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IngestionPlayerGameStatsDto {
    private String athleteEspnId;
    private String statCategory;
    private Map<String, String> stats;
}