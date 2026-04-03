package com.sportstock.league.controller;

import com.sportstock.common.dto.league.CreateInviteRequest;
import com.sportstock.common.dto.league.CreateLeagueRequest;
import com.sportstock.common.dto.league.JoinLeagueRequest;
import com.sportstock.common.dto.league.LeagueInviteResponse;
import com.sportstock.common.dto.league.LeagueMemberResponse;
import com.sportstock.common.dto.league.LeagueResponse;
import com.sportstock.common.dto.league.UpdateInitialStipendStatusRequest;
import com.sportstock.common.security.CurrentUserProvider;
import com.sportstock.league.service.LeagueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api")
public class LeagueController {

  private final LeagueService leagueService;
  private final CurrentUserProvider currentUserProvider;

  @PostMapping("/v1/leagues")
  @ResponseStatus(HttpStatus.CREATED)
  public LeagueResponse createLeague(@Valid @RequestBody CreateLeagueRequest request) {
    return leagueService.createLeague(currentUserProvider.getCurrentUserId(), request);
  }

  @GetMapping("/v1/leagues/{leagueId}")
  @ResponseStatus(HttpStatus.OK)
  public LeagueResponse getLeague(@PathVariable @Positive Long leagueId) {
    return leagueService.getLeague(currentUserProvider.getCurrentUserId(), leagueId);
  }

  @GetMapping("/v1/leagues")
  @ResponseStatus(HttpStatus.OK)
  public Page<LeagueResponse> listMyLeagues(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) int size) {
    return leagueService.listUserLeagues(
        currentUserProvider.getCurrentUserId(), PageRequest.of(page, Math.min(size, 100)));
  }

  @PostMapping("/v1/leagues/{leagueId}/invites")
  @ResponseStatus(HttpStatus.CREATED)
  public LeagueInviteResponse createInvite(
      @PathVariable @Positive Long leagueId, @Valid @RequestBody CreateInviteRequest request) {
    return leagueService.createInvite(currentUserProvider.getCurrentUserId(), leagueId, request);
  }

  @PostMapping("/v1/leagues/{leagueId}/invites/{inviteId}/revoke")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revokeInvite(
      @PathVariable @Positive Long leagueId, @PathVariable @Positive Long inviteId) {
    leagueService.revokeInvite(currentUserProvider.getCurrentUserId(), leagueId, inviteId);
  }

  @PostMapping("/v1/leagues/{leagueId}/join")
  @ResponseStatus(HttpStatus.CREATED)
  public LeagueMemberResponse joinLeague(
      @PathVariable @Positive Long leagueId, @Valid @RequestBody JoinLeagueRequest request) {
    return leagueService.joinLeague(currentUserProvider.getCurrentUserId(), leagueId, request);
  }

  @PostMapping("/v1/leagues/{leagueId}/start")
  @ResponseStatus(HttpStatus.OK)
  public LeagueResponse startLeague(@PathVariable @Positive Long leagueId) {
    return leagueService.startLeague(currentUserProvider.getCurrentUserId(), leagueId);
  }

  @PostMapping("/v1/leagues/{leagueId}/archive")
  @ResponseStatus(HttpStatus.OK)
  public LeagueResponse archiveLeague(@PathVariable @Positive Long leagueId) {
    return leagueService.archiveLeague(currentUserProvider.getCurrentUserId(), leagueId);
  }

  @PostMapping("/v1/leagues/{leagueId}/members/{targetUserId}/remove")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeMember(
      @PathVariable @Positive Long leagueId, @PathVariable @Positive Long targetUserId) {
    leagueService.removeMember(currentUserProvider.getCurrentUserId(), leagueId, targetUserId);
  }

  @PostMapping("/v1/leagues/{leagueId}/leave")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void leaveLeague(@PathVariable @Positive Long leagueId) {
    leagueService.leaveLeague(currentUserProvider.getCurrentUserId(), leagueId);
  }

  @GetMapping("/internal/leagues/{leagueId}/member-ids")
  @ResponseStatus(HttpStatus.OK)
  public List<Long> getMemberUserIds(@PathVariable Long leagueId) {
    return leagueService.getMemberUserIds(leagueId);
  }

  @GetMapping("/internal/leagues/pending-initial-stipend")
  public List<LeagueResponse> getPendingInitialStipend() {
    return leagueService.getLeaguesWithPendingStipend();
  }

  @PutMapping("/internal/leagues/{leagueId}/initial-stipend-status")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateInitialStipendStatus(
      @PathVariable Long leagueId,
      @Valid @RequestBody UpdateInitialStipendStatusRequest request) {
    leagueService.updateInitialStipendStatus(leagueId, request.status());
  }

  @GetMapping("/internal/leagues/active")
  public List<LeagueResponse> getActiveLeagues() {
    return leagueService.getActiveLeagues();
  }

  @GetMapping("/v1/leagues/{leagueId}/members")
  @ResponseStatus(HttpStatus.OK)
  public Page<LeagueMemberResponse> listMembers(
      @PathVariable @Positive Long leagueId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) int size) {
    return leagueService.listMembers(
        currentUserProvider.getCurrentUserId(),
        leagueId,
        PageRequest.of(page, Math.min(size, 100)));
  }
}
