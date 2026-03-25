package com.sportstock.ingestion.controller;

import com.sportstock.ingestion.service.IngestionOrchestrationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/ingestion")
public class IngestionOrchestrationController {

  private static final String ESPN_ID_PATTERN = "\\d{1,15}";

  private final IngestionOrchestrationService orchestrationService;

  @PostMapping("/sync/full")
  public ResponseEntity<Map<String, Object>> syncFull(
      @RequestParam @Min(2000) @Max(2100) Integer seasonYear,
      @RequestParam @Min(1) @Max(4) Integer seasonType,
      @RequestParam @Min(1) @Max(25) Integer week,
      @RequestParam(defaultValue = "false") Boolean force,
      @RequestParam(required = false) @Size(max = 32)
          List<@Pattern(regexp = ESPN_ID_PATTERN) String> teamEspnIds) {
    if (!orchestrationService.tryAcquire(
        IngestionOrchestrationService.SyncType.ADMIN_SYNC)) {
      return rejected("ADMIN_SYNC", "Another sync is running or admin sync is already active");
    }
    try {
      orchestrationService.runAdminSync(seasonYear, seasonType, week, force, teamEspnIds);
    } catch (RejectedExecutionException e) {
      orchestrationService.release(IngestionOrchestrationService.SyncType.ADMIN_SYNC);
      return unavailable("ADMIN_SYNC");
    } catch (Exception e) {
      orchestrationService.release(IngestionOrchestrationService.SyncType.ADMIN_SYNC);
      throw e;
    }
    return ResponseEntity.accepted().body(accepted("ADMIN_SYNC"));
  }

  @PostMapping("/sync/preseason")
  public ResponseEntity<Map<String, Object>> syncPreseason(
      @RequestParam @Min(2000) @Max(2100) Integer seasonYear) {
    if (!orchestrationService.tryAcquire(
        IngestionOrchestrationService.SyncType.PRESEASON_LOAD)) {
      return rejected("PRESEASON_LOAD", "Another sync is running");
    }
    try {
      orchestrationService.runPreseasonLoad(seasonYear);
    } catch (RejectedExecutionException e) {
      orchestrationService.release(IngestionOrchestrationService.SyncType.PRESEASON_LOAD);
      return unavailable("PRESEASON_LOAD");
    } catch (Exception e) {
      orchestrationService.release(IngestionOrchestrationService.SyncType.PRESEASON_LOAD);
      throw e;
    }
    return ResponseEntity.accepted().body(accepted("PRESEASON_LOAD"));
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
