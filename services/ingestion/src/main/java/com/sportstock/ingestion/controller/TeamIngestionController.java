package com.sportstock.ingestion.controller;

import com.sportstock.common.dto.ingestion.TeamRecordResponse;
import com.sportstock.common.dto.ingestion.TeamResponse;
import com.sportstock.common.dto.stock_market.IngestionInjuryStatusDto;
import com.sportstock.ingestion.service.RosterIngestionService;
import com.sportstock.ingestion.service.TeamIngestionService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
public class TeamIngestionController {

  private static final String ESPN_ID_PATTERN = "\\d{1,15}";

  private final TeamIngestionService teamIngestionService;
  private final RosterIngestionService rosterIngestionService;

  @PostMapping("/sync/teams")
  public ResponseEntity<Map<String, Object>> syncTeams() {
    teamIngestionService.ingestTeams();
    return ResponseEntity.accepted().body(accepted("teamsSync"));
  }

  @PostMapping("/sync/teams/details")
  public ResponseEntity<Map<String, Object>> syncAllTeamDetails(
      @RequestParam @Min(2000) @Max(2100) Integer seasonYear) {
    teamIngestionService.ingestAllTeamDetails(seasonYear);
    return ResponseEntity.accepted().body(accepted("allTeamDetailsSync"));
  }

  @PostMapping("/sync/teams/{teamEspnId}")
  public ResponseEntity<Map<String, Object>> syncTeamDetail(
      @PathVariable @NotBlank @Pattern(regexp = ESPN_ID_PATTERN) String teamEspnId,
      @RequestParam @Min(2000) @Max(2100) Integer seasonYear) {
    teamIngestionService.ingestTeamDetail(teamEspnId, seasonYear);
    return ResponseEntity.accepted().body(accepted("teamDetailSync"));
  }

  @PostMapping("/sync/rosters")
  public ResponseEntity<Map<String, Object>> syncRosters(
      @RequestParam @Min(2000) @Max(2100) Integer seasonYear,
      @RequestParam(defaultValue = "200") @Min(1) @Max(500) Integer rosterLimit,
      @RequestParam(required = false) @Size(max = 32)
          List<@Pattern(regexp = ESPN_ID_PATTERN) String> teamEspnIds) {
    rosterIngestionService.ingestAllRosters(seasonYear, rosterLimit, teamEspnIds);
    return ResponseEntity.accepted().body(accepted("rostersSync"));
  }

  @GetMapping("/teams")
  public ResponseEntity<List<TeamResponse>> listTeams() {
    return ResponseEntity.ok(teamIngestionService.listTeams());
  }

  @GetMapping("/teams/{teamEspnId}")
  public ResponseEntity<TeamResponse> getTeam(
      @PathVariable @NotBlank @Pattern(regexp = ESPN_ID_PATTERN) String teamEspnId) {
    return ResponseEntity.ok(teamIngestionService.getTeamByEspnId(teamEspnId));
  }

  @GetMapping("/teams/{teamEspnId}/records")
  public ResponseEntity<List<TeamRecordResponse>> listTeamRecords(
      @PathVariable @NotBlank @Pattern(regexp = ESPN_ID_PATTERN) String teamEspnId,
      @RequestParam @Min(2000) @Max(2100) Integer seasonYear) {
    return ResponseEntity.ok(teamIngestionService.listRecordsByTeam(teamEspnId, seasonYear));
  }

  @GetMapping("/teams/{teamEspnId}/records/{recordType}")
  public ResponseEntity<TeamRecordResponse> getTeamRecord(
      @PathVariable @NotBlank @Pattern(regexp = ESPN_ID_PATTERN) String teamEspnId,
      @PathVariable @NotBlank String recordType,
      @RequestParam @Min(2000) @Max(2100) Integer seasonYear) {
    return ResponseEntity.ok(teamIngestionService.getRecord(teamEspnId, seasonYear, recordType));
  }

  @GetMapping("/rosters/injuries")
  public ResponseEntity<List<IngestionInjuryStatusDto>> listInjuredAthletes(
      @RequestParam @Min(2000) @Max(2100) Integer seasonYear) {
    return ResponseEntity.ok(rosterIngestionService.listInjuredAthletes(seasonYear));
  }

  private Map<String, Object> accepted(String jobName) {
    Map<String, Object> response = new HashMap<>();
    response.put("jobName", jobName);
    response.put("status", "ACCEPTED");
    response.put("requestedAt", Instant.now());
    return response;
  }
}
