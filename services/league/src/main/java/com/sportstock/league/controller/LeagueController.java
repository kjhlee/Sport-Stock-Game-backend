package com.sportstock.league.controller;

import com.sportstock.league.config.CurrentUserProvider;
import com.sportstock.league.dto.request.CreateInviteRequest;
import com.sportstock.league.dto.request.CreateLeagueRequest;
import com.sportstock.league.dto.request.JoinLeagueRequest;
import com.sportstock.league.dto.response.LeagueInviteResponse;
import com.sportstock.league.dto.response.LeagueMemberResponse;
import com.sportstock.league.dto.response.LeagueResponse;
import com.sportstock.league.service.LeagueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/leagues")
public class LeagueController {

    private final LeagueService leagueService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public LeagueResponse createLeague(@Valid @RequestBody CreateLeagueRequest request) {
        return leagueService.createLeague(currentUserProvider.getCurrentUserId(), request);
    }

    @GetMapping("/{leagueId}")
    @ResponseStatus(HttpStatus.OK)
    public LeagueResponse getLeague(@PathVariable Long leagueId) {
        return leagueService.getLeague(currentUserProvider.getCurrentUserId(), leagueId);
    }

    @GetMapping("/")
    @ResponseStatus(HttpStatus.OK)
    public List<LeagueResponse> listMyLeagues() {
        return leagueService.listUserLeagues(currentUserProvider.getCurrentUserId());
    }

    @PostMapping("/{leagueId}/invites")
    @ResponseStatus(HttpStatus.CREATED)
    public LeagueInviteResponse createInvite(@PathVariable Long leagueId,
                                              @Valid @RequestBody CreateInviteRequest request) {
        return leagueService.createInvite(currentUserProvider.getCurrentUserId(), leagueId, request);
    }

    @PostMapping("/{leagueId}/join")
    @ResponseStatus(HttpStatus.CREATED)
    public LeagueMemberResponse joinLeague(@PathVariable Long leagueId,
                                           @Valid @RequestBody JoinLeagueRequest request) {
        return leagueService.joinLeague(currentUserProvider.getCurrentUserId(), leagueId, request);
    }

    @PostMapping("/{leagueId}/start")
    @ResponseStatus(HttpStatus.OK)
    public LeagueResponse startLeague(@PathVariable Long leagueId) {
        return leagueService.startLeague(currentUserProvider.getCurrentUserId(), leagueId);
    }

    @PostMapping("/{leagueId}/members/{targetUserId}/remove")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long leagueId,
                             @PathVariable Long targetUserId) {
        leagueService.removeMember(currentUserProvider.getCurrentUserId(), leagueId, targetUserId);
    }

    @PostMapping("/{leagueId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveLeague(@PathVariable Long leagueId) {
        leagueService.leaveLeague(currentUserProvider.getCurrentUserId(), leagueId);
    }

    @GetMapping("/{leagueId}/members")
    @ResponseStatus(HttpStatus.OK)
    public List<LeagueMemberResponse> listMembers(@PathVariable Long leagueId) {
        return leagueService.listMembers(currentUserProvider.getCurrentUserId(), leagueId);
    }
}
