package com.sportstock.league.service;

import com.sportstock.league.dto.request.CreateInviteRequest;
import com.sportstock.league.dto.request.CreateLeagueRequest;
import com.sportstock.league.dto.request.JoinLeagueRequest;
import com.sportstock.league.dto.response.LeagueInviteResponse;
import com.sportstock.league.dto.response.LeagueMemberResponse;
import com.sportstock.league.dto.response.LeagueResponse;
import com.sportstock.league.entity.League;
import com.sportstock.league.entity.LeagueInvite;
import com.sportstock.league.entity.LeagueMember;
import com.sportstock.league.enums.LeagueStatus;
import com.sportstock.league.exception.InvalidInviteException;
import com.sportstock.league.exception.LeagueAccessDeniedException;
import com.sportstock.league.exception.LeagueNotFoundException;
import com.sportstock.league.exception.LeagueStateException;
import com.sportstock.league.repo.LeagueInviteRepository;
import com.sportstock.league.repo.LeagueMemberRepository;
import com.sportstock.league.repo.LeagueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class LeagueService {

    private final LeagueRepository leagueRepository;
    private final LeagueMemberRepository leagueMemberRepository;
    private final LeagueInviteRepository leagueInviteRepository;

    @Transactional
    public LeagueResponse createLeague(Long userId, CreateLeagueRequest req) {
        League league = new League();
        league.setName(req.name());
        league.setMaxMembers(req.maxMembers());
        league.setSeasonStartAt(req.seasonStartAt());
        league.setSeasonEndAt(req.seasonEndAt());
        league.setOwnerUserId(userId);
        league.setStatus(LeagueStatus.INACTIVE);
        leagueRepository.save(league);

        LeagueMember member = new LeagueMember();
        member.setLeague(league);
        member.setUserId(userId);
        member.setRole("OWNER");
        leagueMemberRepository.save(member);

        return LeagueResponse.from(league, 1);
    }

    @Transactional(readOnly = true)
    public LeagueResponse getLeague(Long userId, Long leagueId) {
        League league = findLeagueOrThrow(leagueId);
        verifyMembership(leagueId, userId);
        int memberCount = leagueMemberRepository.countByLeagueId(leagueId);
        return LeagueResponse.from(league, memberCount);

    }

    @Transactional(readOnly = true)
    public List<LeagueResponse> listUserLeagues(Long userId) {

        List<LeagueMember> memberships = leagueMemberRepository.findByUserId(userId);

        List<Long> leagueIds = memberships.stream()
                .map(m -> m.getLeague().getId())
                .toList();

        return leagueRepository.findAllById(leagueIds).stream()
                .map(league -> LeagueResponse.from(league, leagueMemberRepository.countByLeagueId(league.getId())))
                .toList();

    }

    @Transactional
    public LeagueInviteResponse createInvite(Long userId, Long leagueId, CreateInviteRequest req) {
        League league = findLeagueOrThrow(leagueId);
        verifyOwner(league, userId);

        LeagueInvite invite = new LeagueInvite();

        invite.setCode(UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        invite.setCreatedBy(userId);
        invite.setLeague(league);
        invite.setExpiresAt(req.expiresAt());
        invite.setMaxUses(req.maxUses());
        invite.setUsesCount(0);
        leagueInviteRepository.save(invite);

        return LeagueInviteResponse.from(invite);

    }

    @Transactional
    public LeagueMemberResponse joinLeague(Long userId, Long leagueId, JoinLeagueRequest req) {
        League league = findLeagueOrThrow(leagueId);
        LeagueInvite invite = leagueInviteRepository.findByCodeAndRevokedAtIsNull(req.inviteCode()).orElseThrow(
                () -> new InvalidInviteException("Invalid invite code")
        );

        if (!invite.getLeague().getId().equals(leagueId)) {
            throw new InvalidInviteException("Invite code does not match league");
        }
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidInviteException("Invite code has expired");
        }
        if (invite.getMaxUses() != null && invite.getUsesCount() >= invite.getMaxUses()) {
            throw new InvalidInviteException("Invite code has reached maximum uses");
        }
        if (league.getStartedAt() != null) {
            throw new LeagueStateException("Cannot join a league that has already started");
        }
        if (leagueMemberRepository.countByLeagueId(leagueId) >= league.getMaxMembers()) {
            throw new InvalidInviteException("League is full");
        }
        if (leagueMemberRepository.findByLeagueIdAndUserId(leagueId, userId).isPresent()) {
            throw new LeagueStateException("User is already a member of this league");
        }

        LeagueMember member = new LeagueMember();
        member.setLeague(league);
        member.setUserId(userId);
        member.setRole("MEMBER");
        leagueMemberRepository.save(member);

        invite.setUsesCount(invite.getUsesCount() + 1);
        leagueInviteRepository.save(invite);

        return LeagueMemberResponse.from(member);
    }

    @Transactional
    public LeagueResponse startLeague(Long userId, Long leagueId) {
        // TODO: Add wallet service when implemented
        League league = findLeagueOrThrow(leagueId);
        verifyOwner(league, userId);

        if (league.getStatus() != LeagueStatus.INACTIVE) {
            throw new LeagueStateException("League can only be started when it is inactive");
        }
        league.setStartedAt(OffsetDateTime.now());
        league.setStatus(LeagueStatus.ACTIVE);
        // Call Wallet service and issue initial stipend
        league.setInitialStipendIssuedAt(OffsetDateTime.now());
        leagueRepository.save(league);
        return LeagueResponse.from(league, leagueMemberRepository.countByLeagueId(leagueId));
    }

    @Transactional
    public void removeMember(Long userId, Long leagueId, Long targetUserId) {
        League league = findLeagueOrThrow(leagueId);
        verifyOwner(league, userId);

        if (userId.equals(targetUserId)) {
            throw new LeagueStateException("You cannot remove yourself from the league");
        }
        if (league.getStartedAt() != null) {
            throw new LeagueStateException("Cannot remove members after the league has started");
        }

        LeagueMember member = leagueMemberRepository.findByLeagueIdAndUserId(leagueId, targetUserId).orElseThrow(
                () -> new LeagueNotFoundException("User is not a member of the league")
        );

        leagueMemberRepository.delete(member);
    }

    @Transactional
    public void leaveLeague(Long userId, Long leagueId) {
        League league = findLeagueOrThrow(leagueId);
        LeagueMember member = leagueMemberRepository.findByLeagueIdAndUserId(leagueId, userId).orElseThrow(
                () -> new LeagueNotFoundException("User is not a member of the league")
        );
        if (league.getStartedAt() != null) {
            throw new LeagueStateException("Cannot leave a league that has already started");
        }
        if ("OWNER".equals(member.getRole()) || league.getOwnerUserId().equals(userId)) {
            throw new LeagueStateException("You cannot leave the league if you are the owner");
        }
        leagueMemberRepository.delete(member);

    }

    @Transactional(readOnly = true)
    public List<LeagueMemberResponse> listMembers(Long userId, Long leagueId) {
        // TODO: Implement method per LEAGUE-SERVICE.md
        League league = findLeagueOrThrow(leagueId);
        verifyMembership(leagueId, userId);

        return leagueMemberRepository.findAllByLeagueId(leagueId).stream()
                .map(LeagueMemberResponse::from)
                .toList();
    }

    private League findLeagueOrThrow(Long leagueId) {
        return leagueRepository.findById(leagueId).orElseThrow(
                () -> new LeagueNotFoundException("League not found: " + leagueId)
        );
    }

    private void verifyOwner(League league, Long userId) {
        Long ownerId = league.getOwnerUserId();
        if (!ownerId.equals(userId)) {
            log.warn("Owner verification failed for leagueId={} callerUserId={} ownerUserId={}",
                    league.getId(), userId, ownerId);
            throw new LeagueAccessDeniedException("Only the league owner can perform this action");
        }
        log.debug("Owner verification passed for leagueId={} userId={}", league.getId(), userId);
    }

    private void verifyMembership(Long leagueId, Long userId) {
        Optional<LeagueMember> member = leagueMemberRepository.findByLeagueIdAndUserId(leagueId, userId);
        if (member.isEmpty()) {
            log.warn("Membership verification failed for leagueId={} userId={}", leagueId, userId);
            throw new LeagueAccessDeniedException("You are not a member of this league");
        }
        log.debug("Membership verification passed for leagueId={} userId={}", leagueId, userId);
    }
}
