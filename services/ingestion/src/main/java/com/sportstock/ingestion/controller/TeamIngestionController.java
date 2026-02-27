package com.sportstock.ingestion.controller;

import com.sportstock.ingestion.entity.Team;
import com.sportstock.ingestion.service.RosterIngestionService;
import com.sportstock.ingestion.service.TeamIngestionService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/ingestion")
public class TeamIngestionController {

    private final TeamIngestionService teamIngestionService;
    private final RosterIngestionService rosterIngestionService;

    @PostMapping("/sync/teams")
    public ResponseEntity<Map<String, Object>> syncTeams() {
        teamIngestionService.ingestTeams();
        return ResponseEntity.accepted().body(accepted("teamsSync"));
    }

    @PostMapping("/sync/teams/{teamEspnId}")
    public ResponseEntity<Map<String, Object>> syncTeamDetail(
            @PathVariable @NotBlank String teamEspnId
    ) {
        teamIngestionService.ingestTeamDetail(teamEspnId);
        return ResponseEntity.accepted().body(accepted("teamDetailSync"));
    }

    @PostMapping("/sync/rosters")
    public ResponseEntity<Map<String, Object>> syncRosters(
            @RequestParam @Min(2000) @Max(2100) Integer seasonYear,
            @RequestParam(defaultValue = "200") @Min(1) @Max(500) Integer rosterLimit,
            @RequestParam(required = false) List<String> teamEspnIds
    ) {
        rosterIngestionService.ingestAllRosters(seasonYear, rosterLimit, teamEspnIds);
        return ResponseEntity.accepted().body(accepted("rostersSync"));
    }

    @GetMapping("/teams")
    public ResponseEntity<List<Team>> listTeams() {
        return ResponseEntity.ok(teamIngestionService.listTeams());
    }

    @GetMapping("/teams/{teamEspnId}")
    public ResponseEntity<Team> getTeam(
            @PathVariable @NotBlank String teamEspnId
    ) {
        return ResponseEntity.ok(teamIngestionService.getTeamByEspnId(teamEspnId));
    }

    private Map<String, Object> accepted(String jobName) {
        Map<String, Object> response = new HashMap<>();
        response.put("jobName", jobName);
        response.put("status", "ACCEPTED");
        response.put("requestedAt", Instant.now());
        return response;
    }
}
