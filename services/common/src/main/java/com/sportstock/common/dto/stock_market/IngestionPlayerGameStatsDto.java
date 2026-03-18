package com.sportstock.common.dto.stock_market;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
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
