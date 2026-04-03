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
  private String statusState;

  public IngestionEventDto(
      String espnId,
      Integer seasonYear,
      Integer seasonType,
      Integer weekNumber,
      Boolean statusCompleted,
      String statusState) {
    this.espnId = espnId;
    this.seasonYear = seasonYear;
    this.seasonType = seasonType;
    this.weekNumber = weekNumber;
    this.statusCompleted = statusCompleted;
    this.statusState = statusState;
  }
}
