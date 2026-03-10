package com.sportstock.ingestion.controller;

import com.sportstock.common.dto.ingestion.AthleteResponse;
import com.sportstock.ingestion.service.AthleteIngestionService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class AthleteIngestionController {

    private static final String ESPN_ID_PATTERN = "\\d{1,15}";

    private final AthleteIngestionService athleteIngestionService;

    @PostMapping("/sync/athletes")
    public ResponseEntity<Map<String, Object>> syncAthletes(
            @RequestParam(defaultValue = "500") @Min(1) @Max(1000) Integer pageSize,
            @RequestParam(defaultValue = "40") @Min(1) @Max(10000) Integer pageCount
    ) {
        athleteIngestionService.ingestAthletes(pageSize, pageCount);
        return ResponseEntity.accepted().body(accepted("athletesSync"));
    }

    @GetMapping("/athletes")
    public ResponseEntity<List<AthleteResponse>> listAthletes(
            @RequestParam(required = false) String positionAbbreviation,
            @RequestParam(defaultValue = "false") boolean includeStubs
    ) {
        return ResponseEntity.ok(athleteIngestionService.listAthletes(positionAbbreviation, includeStubs));
    }

    @GetMapping("/athletes/{athleteEspnId}")
    public ResponseEntity<AthleteResponse> getAthlete(
            @PathVariable @NotBlank @Pattern(regexp = ESPN_ID_PATTERN) String athleteEspnId
    ) {
        return ResponseEntity.ok(athleteIngestionService.getAthleteByEspnId(athleteEspnId));
    }

    private Map<String, Object> accepted(String jobName) {
        Map<String, Object> response = new HashMap<>();
        response.put("jobName", jobName);
        response.put("status", "ACCEPTED");
        response.put("requestedAt", Instant.now());
        return response;
    }
}
