package com.sportstock.league.service;

import com.sportstock.common.dto.league.CreateInviteRequest;
import com.sportstock.common.dto.league.CreateLeagueRequest;
import com.sportstock.common.dto.league.JoinLeagueRequest;
import com.sportstock.common.dto.league.LeagueInviteResponse;
import com.sportstock.common.dto.league.LeagueMemberResponse;
import com.sportstock.common.dto.league.LeagueResponse;
import com.sportstock.league.mapper.DtoMapper;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
        league.setInitialStipendAmount(req.initialStipendAmount());
        league.setWeeklyStipendAmount(req.weeklyStipendAmount());
        league.setWeeklyPayoutDowUtc(req.weeklyPayoutDowUtc());
        leagueRepository.save(league);

        LeagueMember member = new LeagueMember();
        member.setLeague(league);
        member.setUserId(userId);
        member.setRole("OWNER");
        leagueMemberRepository.save(member);

        return DtoMapper.toLeagueResponse(league, 1);
    }

    @Transactional(readOnly = true)
    public LeagueResponse getLeague(Long userId, Long leagueId) {
        League league = findLeagueOrThrow(leagueId);
        verifyMembership(leagueId, userId);
        int memberCount = leagueMemberRepository.countByLeagueId(leagueId);
        return DtoMapper.toLeagueResponse(league, memberCount);
    }

    @Transactional(readOnly = true)
    public Page<LeagueResponse> listUserLeagues(Long userId, Pageable pageable) {
        Page<LeagueMember> memberships = leagueMemberRepository.findByUserId(userId, pageable);

        List<Long> leagueIds = memberships.stream()
                .map(m -> m.getLeague().getId())
                .toList();

        if (leagueIds.isEmpty()) {
            return memberships.map(m -> null);
        }

        Map<Long, League> leagueMap = leagueRepository.findAllById(leagueIds).stream()
                .collect(Collectors.toMap(League::getId, l -> l));

        Map<Long, Long> countMap = leagueMemberRepository.countByLeagueIds(leagueIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        return memberships.map(m -> {
            League league = leagueMap.get(m.getLeague().getId());
            return DtoMapper.toLeagueResponse(league, countMap.getOrDefault(league.getId(), 0L).intValue());
        });
    }

    @Transactional
    public LeagueInviteResponse createInvite(Long userId, Long leagueId, CreateInviteRequest req) {
        League league = findLeagueOrThrow(leagueId);
        verifyOwner(league, userId);

        if (league.getStatus() != LeagueStatus.INACTIVE) {
            throw new LeagueStateException("Cannot create invites for a league that is not inactive");
        }

        LeagueInvite invite = new LeagueInvite();

        invite.setCode(generateUniqueInviteCode());
        invite.setCreatedBy(userId);
        invite.setLeague(league);
        invite.setExpiresAt(req.expiresAt());
        invite.setMaxUses(req.maxUses());
        invite.setUsesCount(0);
        leagueInviteRepository.save(invite);

        return DtoMapper.toLeagueInviteResponse(invite);
    }

    @Transactional
    public LeagueMemberResponse joinLeague(Long userId, Long leagueId, JoinLeagueRequest req) {
        League league = leagueRepository.findByIdForUpdate(leagueId)
                .orElseThrow(() -> new LeagueNotFoundException("League not found: " + leagueId));
        LeagueInvite invite = leagueInviteRepository.findByCodeAndRevokedAtIsNull(req.inviteCode()).orElseThrow(
                () -> new InvalidInviteException("Invalid invite code")
        );

        if (!invite.getLeague().getId().equals(leagueId)) {
            throw new InvalidInviteException("Invite code does not match league");
        }
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidInviteException("Invite code has expired");
        }
        if (league.getStartedAt() != null || league.getStatus() != LeagueStatus.INACTIVE) {
            throw new LeagueStateException("Cannot join a league that has already started");
        }
        if (leagueMemberRepository.countByLeagueId(leagueId) >= league.getMaxMembers()) {
            throw new LeagueStateException("League is full");
        }
        if (leagueMemberRepository.findByLeagueIdAndUserId(leagueId, userId).isPresent()) {
            throw new LeagueStateException("User is already a member of this league");
        }

        int updated = leagueInviteRepository.incrementUsesCount(invite.getId());
        if (updated == 0) {
            throw new InvalidInviteException("Invite code has reached maximum uses");
        }

        LeagueMember member = new LeagueMember();
        member.setLeague(league);
        member.setUserId(userId);
        member.setRole("MEMBER");
        leagueMemberRepository.save(member);


        return DtoMapper.toLeagueMemberResponse(member);
    }

    @Transactional
    public LeagueResponse startLeague(Long userId, Long leagueId) {
        // TODO: Add wallet service when implemented
        League league = findLeagueOrThrow(leagueId);
        verifyOwner(league, userId);

        if (league.getStatus() != LeagueStatus.INACTIVE) {
            throw new LeagueStateException("League can only be started when it is inactive");
        }

        int memberCount = leagueMemberRepository.countByLeagueId(leagueId);
        if (memberCount < 2) {
            throw new LeagueStateException("League must have at least 2 members to start");
        }

        league.setStartedAt(OffsetDateTime.now());
        league.setStatus(LeagueStatus.ACTIVE);
        // Call Wallet service and issue initial stipend
        league.setInitialStipendIssuedAt(OffsetDateTime.now());
        leagueRepository.save(league);
        return DtoMapper.toLeagueResponse(league, memberCount);
    }

    @Transactional
    public void removeMember(Long userId, Long leagueId, Long targetUserId) {
        League league = findLeagueOrThrow(leagueId);
        verifyOwner(league, userId);

        if (userId.equals(targetUserId)) {
            throw new LeagueStateException("You cannot remove yourself from the league");
        }
        if (league.getStartedAt() != null || league.getStatus() != LeagueStatus.INACTIVE) {
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
        if (league.getStartedAt() != null || league.getStatus() != LeagueStatus.INACTIVE) {
            throw new LeagueStateException("Cannot leave a league that has already started");
        }
        if ("OWNER".equals(member.getRole()) || league.getOwnerUserId().equals(userId)) {
            throw new LeagueStateException("You cannot leave the league if you are the owner");
        }
        leagueMemberRepository.delete(member);
    }

    @Transactional(readOnly = true)
    public Page<LeagueMemberResponse> listMembers(Long userId, Long leagueId, Pageable pageable) {
        findLeagueOrThrow(leagueId);
        verifyMembership(leagueId, userId);

        return leagueMemberRepository.findAllByLeagueId(leagueId, pageable)
                .map(DtoMapper::toLeagueMemberResponse);
    }

    @Transactional
    public void revokeInvite(Long userId, Long leagueId, Long inviteId) {
        League league = findLeagueOrThrow(leagueId);
        verifyOwner(league, userId);

        LeagueInvite invite = leagueInviteRepository.findById(inviteId)
                .orElseThrow(() -> new LeagueNotFoundException("Invite not found: " + inviteId));

        if (!invite.getLeague().getId().equals(leagueId)) {
            throw new LeagueAccessDeniedException("Invite does not belong to this league");
        }
        if (invite.getRevokedAt() != null) {
            throw new LeagueStateException("Invite is already revoked");
        }

        invite.setRevokedAt(OffsetDateTime.now());
        leagueInviteRepository.save(invite);
    }

    @Transactional
    public LeagueResponse archiveLeague(Long userId, Long leagueId) {
        League league = findLeagueOrThrow(leagueId);
        verifyOwner(league, userId);

        if (league.getStatus() == LeagueStatus.ARCHIVED) {
            throw new LeagueStateException("League is already archived");
        }

        league.setStatus(LeagueStatus.ARCHIVED);
        leagueRepository.save(league);
        return DtoMapper.toLeagueResponse(league, leagueMemberRepository.countByLeagueId(leagueId));
    }

    private League findLeagueOrThrow(Long leagueId) {
        return leagueRepository.findById(leagueId).orElseThrow(
                () -> new LeagueNotFoundException("League not found: " + leagueId)
        );
    }

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < 3; attempt++) {
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
            if (leagueInviteRepository.findByCode(code).isEmpty()) {
                return code;
            }
        }
        throw new LeagueStateException("Failed to generate unique invite code");
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
        if (leagueMemberRepository.findByLeagueIdAndUserId(leagueId, userId).isEmpty()) {
            log.warn("Membership verification failed for leagueId={} userId={}", leagueId, userId);
            throw new LeagueAccessDeniedException("You are not a member of this league");
        }
        log.debug("Membership verification passed for leagueId={} userId={}", leagueId, userId);
    }
}
