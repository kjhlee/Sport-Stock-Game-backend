package com.sportstock.common.dto.stock_market;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IngestionEventDto {
  private String espnId;
  private int seasonYear;
  private Integer seasonType;
  private Integer weekNumber;
  private Boolean statusCompleted;
}
