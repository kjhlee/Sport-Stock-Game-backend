package com.sportstock.league.controller;

import com.sportstock.common.dto.league.CreateInviteRequest;
import com.sportstock.common.dto.league.CreateLeagueRequest;
import com.sportstock.common.dto.league.JoinLeagueRequest;
import com.sportstock.common.dto.league.LeagueInviteResponse;
import com.sportstock.common.dto.league.LeagueMemberResponse;
import com.sportstock.common.dto.league.LeagueResponse;
import com.sportstock.common.security.CurrentUserProvider;
import com.sportstock.league.service.LeagueService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/leagues")
public class LeagueController {

  private final LeagueService leagueService;
  private final CurrentUserProvider currentUserProvider;

  @PostMapping("")
  @ResponseStatus(HttpStatus.CREATED)
  public LeagueResponse createLeague(@Valid @RequestBody CreateLeagueRequest request) {
    return leagueService.createLeague(currentUserProvider.getCurrentUserId(), request);
  }

  @GetMapping("/{leagueId}")
  @ResponseStatus(HttpStatus.OK)
  public LeagueResponse getLeague(@PathVariable Long leagueId) {
    return leagueService.getLeague(currentUserProvider.getCurrentUserId(), leagueId);
  }

  @GetMapping("")
  @ResponseStatus(HttpStatus.OK)
  public Page<LeagueResponse> listMyLeagues(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return leagueService.listUserLeagues(
        currentUserProvider.getCurrentUserId(), PageRequest.of(page, Math.min(size, 100)));
  }

  @PostMapping("/{leagueId}/invites")
  @ResponseStatus(HttpStatus.CREATED)
  public LeagueInviteResponse createInvite(
      @PathVariable Long leagueId, @Valid @RequestBody CreateInviteRequest request) {
    return leagueService.createInvite(currentUserProvider.getCurrentUserId(), leagueId, request);
  }

  @PostMapping("/{leagueId}/invites/{inviteId}/revoke")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revokeInvite(@PathVariable Long leagueId, @PathVariable Long inviteId) {
    leagueService.revokeInvite(currentUserProvider.getCurrentUserId(), leagueId, inviteId);
  }

  @PostMapping("/{leagueId}/join")
  @ResponseStatus(HttpStatus.CREATED)
  public LeagueMemberResponse joinLeague(
      @PathVariable Long leagueId, @Valid @RequestBody JoinLeagueRequest request) {
    return leagueService.joinLeague(currentUserProvider.getCurrentUserId(), leagueId, request);
  }

  @PostMapping("/{leagueId}/start")
  @ResponseStatus(HttpStatus.OK)
  public LeagueResponse startLeague(@PathVariable Long leagueId) {
    return leagueService.startLeague(currentUserProvider.getCurrentUserId(), leagueId);
  }

  @PostMapping("/{leagueId}/archive")
  @ResponseStatus(HttpStatus.OK)
  public LeagueResponse archiveLeague(@PathVariable Long leagueId) {
    return leagueService.archiveLeague(currentUserProvider.getCurrentUserId(), leagueId);
  }

  @PostMapping("/{leagueId}/members/{targetUserId}/remove")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeMember(@PathVariable Long leagueId, @PathVariable Long targetUserId) {
    leagueService.removeMember(currentUserProvider.getCurrentUserId(), leagueId, targetUserId);
  }

  @PostMapping("/{leagueId}/leave")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void leaveLeague(@PathVariable Long leagueId) {
    leagueService.leaveLeague(currentUserProvider.getCurrentUserId(), leagueId);
  }

  @GetMapping("/internal/{leagueId}/member-ids")
  @ResponseStatus(HttpStatus.OK)
  public List<Long> getMemberUserIds(@PathVariable Long leagueId) {
    return leagueService.getMemberUserIds(leagueId);
  }

  @GetMapping("/pending-initial-stipend")
  public List<LeagueResponse> getPendingInitialStipend() {
    return leagueService.getLeaguesWithPendingStipend();
  }

  @PutMapping("/{leagueId}/initial-stipend-status")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateInitialStipendStatus(
      @PathVariable Long leagueId, @RequestBody Map<String, String> body) {
    leagueService.updateInitialStipendStatus(leagueId, body.get("status"));
  }

  @GetMapping("/active")
  public List<LeagueResponse> getActiveLeagues() {
    return leagueService.getActiveLeagues();
  }

  @GetMapping("/{leagueId}/members")
  @ResponseStatus(HttpStatus.OK)
  public Page<LeagueMemberResponse> listMembers(
      @PathVariable Long leagueId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return leagueService.listMembers(
        currentUserProvider.getCurrentUserId(),
        leagueId,
        PageRequest.of(page, Math.min(size, 100)));
  }
}
