package com.sportstocks.stockmarket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IngestionTeamDto {
    private String espnId;
    private String displayName;
    private String abbreviation;
}