package com.sportstock.ingestion.controller;

import com.sportstock.ingestion.service.IngestionOrchestrationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/ingestion")
public class IngestionOrchestrationController {

    private static final String ESPN_ID_PATTERN = "\\d{1,15}";

    private final IngestionOrchestrationService ingestionOrchestrationService;

    @PostMapping("/sync/foundation")
    public ResponseEntity<Map<String, Object>> syncFoundation(
            @RequestParam @Min(2000) @Max(2100) Integer seasonYear,
            @RequestParam @Min(1) @Max(4) Integer seasonType,
            @RequestParam @Min(1) @Max(25) Integer week
    ) {
        if (!ingestionOrchestrationService.tryStartFoundationSync(seasonYear, seasonType, week)) {
            return rejected("foundationSync", "Another sync job is already running for this season window or a full sync is in progress");
        }
        try {
            ingestionOrchestrationService.runFoundationSync(seasonYear, seasonType, week);
        } catch (RejectedExecutionException e) {
            ingestionOrchestrationService.cancelFoundationSyncStart(seasonYear, seasonType, week);
            return unavailable("foundationSync");
        }
        return ResponseEntity.accepted().body(accepted("foundationSync"));
    }

    @PostMapping("/sync/weekly")
    public ResponseEntity<Map<String, Object>> syncWeekly(
            @RequestParam @Min(2000) @Max(2100) Integer seasonYear,
            @RequestParam @Min(1) @Max(4) Integer seasonType,
            @RequestParam @Min(1) @Max(25) Integer week
    ) {
        if (!ingestionOrchestrationService.tryStartWeeklySync(seasonYear, seasonType, week)) {
            return rejected("weeklySync", "Another sync job is already running for this season window or a full sync is in progress");
        }
        try {
            ingestionOrchestrationService.runWeeklySync(seasonYear, seasonType, week);
        } catch (RejectedExecutionException e) {
            ingestionOrchestrationService.cancelWeeklySyncStart(seasonYear, seasonType, week);
            return unavailable("weeklySync");
        }
        return ResponseEntity.accepted().body(accepted("weeklySync"));
    }

    @PostMapping("/sync/full")
    public ResponseEntity<Map<String, Object>> syncFull(
            @RequestParam @Min(2000) @Max(2100) Integer seasonYear,
            @RequestParam @Min(1) @Max(4) Integer seasonType,
            @RequestParam @Min(1) @Max(25) Integer week,
            @RequestParam(defaultValue = "200") @Min(1) @Max(500) Integer rosterLimit,
            @RequestParam(defaultValue = "100") @Min(1) @Max(1000) Integer athletePageSize,
            @RequestParam(defaultValue = "1") @Min(1) @Max(10000) Integer athletePageCount,
            @RequestParam(required = false) @Size(max = 32) List<@Pattern(regexp = ESPN_ID_PATTERN) String> teamEspnIds
    ) {
        if (!ingestionOrchestrationService.tryStartFullSync()) {
            return rejected("fullSync", "Another full sync is already running or window jobs are active");
        }
        try {
            ingestionOrchestrationService.runFullSync(
                    seasonYear, seasonType, week,
                    rosterLimit, athletePageSize, athletePageCount, teamEspnIds
            );
        } catch (RejectedExecutionException e) {
            ingestionOrchestrationService.cancelFullSyncStart();
            return unavailable("fullSync");
        }
        return ResponseEntity.accepted().body(accepted("fullSync"));
    }

    private Map<String, Object> accepted(String jobName) {
        Map<String, Object> response = new HashMap<>();
        response.put("jobName", jobName);
        response.put("status", "ACCEPTED");
        response.put("requestedAt", Instant.now());
        return response;
    }

    private ResponseEntity<Map<String, Object>> rejected(String jobName, String reason) {
        Map<String, Object> response = new HashMap<>();
        response.put("jobName", jobName);
        response.put("status", "REJECTED_BUSY");
        response.put("reason", reason);
        response.put("requestedAt", Instant.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    private ResponseEntity<Map<String, Object>> unavailable(String jobName) {
        Map<String, Object> response = new HashMap<>();
        response.put("jobName", jobName);
        response.put("status", "SERVICE_UNAVAILABLE");
        response.put("reason", "Ingestion thread pool is saturated, try again later");
        response.put("requestedAt", Instant.now());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
