package com.sportstocks.stockmarket.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IngestionAthleteDto {
    private String espnId;
    private String fullName;
    private String positionAbbreviation;
    private String statusType;
    private String teamEspnId;
}