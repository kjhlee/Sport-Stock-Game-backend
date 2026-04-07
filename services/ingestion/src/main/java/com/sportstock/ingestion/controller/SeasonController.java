package com.sportstock.ingestion.controller;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import com.sportstock.ingestion.service.SeasonQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/ingestion/seasons")
@RequiredArgsConstructor
public class SeasonController {

  private final SeasonQueryService seasonQueryService;

  @GetMapping("/current-week")
  @ResponseStatus(HttpStatus.OK)
  public CurrentWeekResponse getCurrentWeek() {
    return seasonQueryService.getCurrentWeek();
  }

  @GetMapping("/season-active")
  public ResponseEntity<?> isSeasonActive() {
    boolean active = seasonQueryService.isSeasonActive();
    var response = new java.util.LinkedHashMap<String, Object>();
    response.put("active", active);
    if (active) {
      var week = seasonQueryService.getCurrentWeek();
      response.put("seasonYear", week.seasonYear());
      response.put("seasonType", week.seasonType());
    }
    return ResponseEntity.ok(response);
  }

  @GetMapping("/current-week/optional")
  public ResponseEntity<CurrentWeekResponse> getCurrentWeekOptional() {
    return seasonQueryService
        .getCurrentWeekOptional()
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.noContent().build());
  }

  @GetMapping("/prior-week")
  public ResponseEntity<CurrentWeekResponse> getPriorWeek() {
    return seasonQueryService
        .getPriorWeek()
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.noContent().build());
  }

  @GetMapping("/current-week-or-preseason/optional")
  public ResponseEntity<CurrentWeekResponse> getCurrentWeekIncludingPreseasonOptional() {
    return seasonQueryService
        .getCurrentWeekIncludingPreseasonOptional()
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.noContent().build());
  }
}
