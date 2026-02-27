package com.sportstock.ingestion.controller;

import com.sportstock.ingestion.service.IngestionOrchestrationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
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
        ingestionOrchestrationService.runFoundationSync(seasonYear, seasonType, week);
        return ResponseEntity.accepted().body(accepted("foundationSync"));
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
        ingestionOrchestrationService.runFullSync(
                seasonYear,
                seasonType,
                week,
                rosterLimit,
                athletePageSize,
                athletePageCount,
                teamEspnIds
        );
        return ResponseEntity.accepted().body(accepted("fullSync"));
    }

    private Map<String, Object> accepted(String jobName) {
        Map<String, Object> response = new HashMap<>();
        response.put("jobName", jobName);
        response.put("status", "ACCEPTED");
        response.put("requestedAt", Instant.now());
        return response;
    }
}
