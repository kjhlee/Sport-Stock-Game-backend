package com.sportstock.ingestion.controller;

import com.sportstock.common.dto.ingestion.FantasySnapshotResponse;
import com.sportstock.ingestion.entity.FantasySnapshot;
import com.sportstock.ingestion.repo.FantasySnapshotRepository;
import com.sportstock.ingestion.service.FantasySnapshotIngestionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
public class FantasySnapshotController {

  private final FantasySnapshotIngestionService fantasySnapshotIngestionService;
  private final FantasySnapshotRepository fantasySnapshotRepository;

  @PostMapping("/sync/projections")
  public ResponseEntity<?> syncProjections(
      @RequestParam int seasonYear, @RequestParam int seasonType, @RequestParam int weekNumber) {
    var result =
        fantasySnapshotIngestionService.ingestProjections(seasonYear, seasonType, weekNumber);
    return ResponseEntity.ok(result);
  }

  @PostMapping("/sync/actual-fantasy-points")
  public ResponseEntity<?> syncActualFantasyPoints(@RequestParam String eventEspnId) {
    var result = fantasySnapshotIngestionService.ingestActualFantasyPoints(eventEspnId);
    return ResponseEntity.ok(result);
  }

  @PostMapping("/sync/mark-event-completed")
  public ResponseEntity<?> markEventCompleted(@RequestParam String eventEspnId) {
    int count = fantasySnapshotIngestionService.markEventCompleted(eventEspnId);
    return ResponseEntity.ok(java.util.Map.of("markedCompleted", count));
  }

  @GetMapping("/fantasy-snapshots")
  public List<FantasySnapshotResponse> getByEvent(@RequestParam String eventEspnId) {
    return fantasySnapshotRepository.findByEventEspnId(eventEspnId).stream()
        .map(this::toResponse)
        .toList();
  }

  @GetMapping("/fantasy-snapshots/by-espn-id")
  public ResponseEntity<FantasySnapshotResponse> getByEspnId(
      @RequestParam String espnId,
      @RequestParam String subjectType,
      @RequestParam int seasonYear,
      @RequestParam int seasonType,
      @RequestParam int weekNumber) {
    return fantasySnapshotRepository
        .findByEspnIdAndSubjectTypeAndWeek(
            espnId, subjectType, seasonYear, seasonType, weekNumber)
        .map(this::toResponse)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  private FantasySnapshotResponse toResponse(FantasySnapshot fs) {
    return new FantasySnapshotResponse(
        fs.getId(),
        fs.getEvent().getEspnId(),
        fs.getSubjectType(),
        fs.getEspnId(),
        fs.getFullName(),
        fs.getProjectedStats(),
        fs.getProjectedFantasyPoints(),
        fs.getActualFantasyPoints(),
        fs.isCompleted(),
        fs.getUpdatedAt());
  }
}
