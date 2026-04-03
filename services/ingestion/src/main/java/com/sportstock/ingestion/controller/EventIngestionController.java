package com.sportstock.ingestion.controller;

import com.sportstock.common.dto.ingestion.BoxscoreTeamStatResponse;
import com.sportstock.common.dto.ingestion.EventResponse;
import com.sportstock.common.dto.ingestion.PlayerGameStatResponse;
import com.sportstock.common.dto.stock_market.IngestionEventDto;
import com.sportstock.ingestion.entity.Event;
import com.sportstock.ingestion.repo.EventCompetitorRepository;
import com.sportstock.ingestion.repo.EventRepository;
import com.sportstock.ingestion.repo.TeamRosterEntryRepository;
import com.sportstock.ingestion.service.EventIngestionService;
import com.sportstock.ingestion.service.EventSummaryIngestionService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/internal/ingestion")
public class EventIngestionController {

  private static final String ESPN_ID_PATTERN = "\\d{1,15}";

  private final EventIngestionService eventIngestionService;
  private final EventSummaryIngestionService eventSummaryIngestionService;
  private final EventRepository eventRepository;
  private final EventCompetitorRepository eventCompetitorRepository;
  private final TeamRosterEntryRepository teamRosterEntryRepository;

  @PostMapping("/sync/scoreboard")
  public ResponseEntity<Map<String, Object>> syncScoreboard(
      @RequestParam @Min(2000) @Max(2100) Integer seasonYear,
      @RequestParam @Min(1) @Max(4) Integer seasonType,
      @RequestParam @Min(1) @Max(25) Integer week) {
    eventIngestionService.ingestScoreboard(seasonYear, seasonType, week);
    return ResponseEntity.accepted().body(accepted("scoreboardSync"));
  }

  @PostMapping("/sync/events/{eventEspnId}/summary")
  public ResponseEntity<Map<String, Object>> syncEventSummary(
      @PathVariable @NotBlank @Pattern(regexp = ESPN_ID_PATTERN) String eventEspnId) {
    eventSummaryIngestionService.ingestEventSummary(eventEspnId);
    return ResponseEntity.accepted().body(accepted("eventSummarySync"));
  }

  @GetMapping("/events")
  public ResponseEntity<List<EventResponse>> listEvents(
      @RequestParam @Min(2000) @Max(2100) Integer seasonYear,
      @RequestParam(required = false) @Min(1) @Max(4) Integer seasonType,
      @RequestParam(required = false) @Min(1) @Max(25) Integer weekNumber) {
    if (weekNumber != null && seasonType == null) {
      throw new IllegalArgumentException("seasonType is required when weekNumber is provided");
    }
    return ResponseEntity.ok(eventIngestionService.listEvents(seasonYear, seasonType, weekNumber));
  }

  @GetMapping("/events/{eventEspnId}")
  public ResponseEntity<IngestionEventDto> getEvent(
      @PathVariable @NotBlank @Pattern(regexp = ESPN_ID_PATTERN) String eventEspnId) {
    return ResponseEntity.ok(eventIngestionService.getEventByEspnId(eventEspnId));
  }

  @GetMapping("/events/{eventEspnId}/team-stats")
  public ResponseEntity<List<BoxscoreTeamStatResponse>> getEventTeamStats(
      @PathVariable @NotBlank @Pattern(regexp = ESPN_ID_PATTERN) String eventEspnId) {
    return ResponseEntity.ok(eventSummaryIngestionService.getTeamStats(eventEspnId));
  }

  @GetMapping("/events/{eventEspnId}/player-stats")
  public ResponseEntity<List<PlayerGameStatResponse>> getEventPlayerStats(
      @PathVariable @NotBlank @Pattern(regexp = ESPN_ID_PATTERN) String eventEspnId,
      @RequestParam(required = false) @Pattern(regexp = ESPN_ID_PATTERN) String teamEspnId) {
    return ResponseEntity.ok(eventSummaryIngestionService.getPlayerStats(eventEspnId, teamEspnId));
  }

  @GetMapping("/events/{eventEspnId}/player-stats/{athleteEspnId}")
  public ResponseEntity<List<PlayerGameStatResponse>> getEventPlayerStatsByAthlete(
      @PathVariable @NotBlank @Pattern(regexp = ESPN_ID_PATTERN) String eventEspnId,
      @PathVariable @NotBlank @Pattern(regexp = ESPN_ID_PATTERN) String athleteEspnId) {
    return ResponseEntity.ok(
        eventSummaryIngestionService.getPlayerStatsByAthlete(eventEspnId, athleteEspnId));
  }

  private Map<String, Object> accepted(String jobName) {
    Map<String, Object> response = new HashMap<>();
    response.put("jobName", jobName);
    response.put("status", "ACCEPTED");
    response.put("requestedAt", Instant.now());
    return response;
  }

  @GetMapping("/events/{espnId}/teams")
  public List<String> getEventTeamEspnIds(@PathVariable String espnId) {
    return eventCompetitorRepository.findByEventEspnId(espnId).stream()
        .map(ec -> ec.getTeam().getEspnId())
        .toList();
  }

  @GetMapping("/events/{espnId}/roster/{teamEspnId}")
  public List<String> getEventRosterEspnIds(
      @PathVariable String espnId, @PathVariable String teamEspnId) {
    Event event =
        eventRepository
            .findByEspnId(espnId)
            .orElseThrow(() -> new IllegalArgumentException("Event not found: " + espnId));
    return teamRosterEntryRepository
        .findByTeamEspnIdAndSeasonYear(teamEspnId, event.getSeasonYear())
        .stream()
        .map(entry -> entry.getAthlete().getEspnId())
        .toList();
  }
}
