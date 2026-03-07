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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeagueServiceTest {

    private static final BigDecimal STIPEND = new BigDecimal("10000.00");
    private static final BigDecimal WEEKLY_STIPEND = new BigDecimal("500.00");

    @Mock
    private LeagueRepository leagueRepository;
    @Mock
    private LeagueMemberRepository leagueMemberRepository;
    @Mock
    private LeagueInviteRepository leagueInviteRepository;

    private LeagueService service;

    @BeforeEach
    void setUp() {
        service = new LeagueService(leagueRepository, leagueMemberRepository, leagueInviteRepository);
    }

    @Test
    void createLeagueSetsInactiveStatusAndOwnerMembership() {
        CreateLeagueRequest req = createLeagueRequest();
        when(leagueRepository.save(any(League.class))).thenAnswer(inv -> {
            League league = inv.getArgument(0);
            league.setId(100L);
            return league;
        });
        when(leagueMemberRepository.save(any(LeagueMember.class))).thenAnswer(inv -> {
            LeagueMember member = inv.getArgument(0);
            member.setId(200L);
            return member;
        });

        LeagueResponse response = service.createLeague(10L, req);

        ArgumentCaptor<League> leagueCaptor = ArgumentCaptor.forClass(League.class);
        verify(leagueRepository).save(leagueCaptor.capture());
        League savedLeague = leagueCaptor.getValue();
        assertEquals(LeagueStatus.INACTIVE, savedLeague.getStatus());
        assertEquals(10L, savedLeague.getOwnerUserId());
        assertEquals(req.name(), savedLeague.getName());
        assertEquals(req.maxMembers(), savedLeague.getMaxMembers());

        ArgumentCaptor<LeagueMember> memberCaptor = ArgumentCaptor.forClass(LeagueMember.class);
        verify(leagueMemberRepository).save(memberCaptor.capture());
        LeagueMember savedMember = memberCaptor.getValue();
        assertEquals(10L, savedMember.getUserId());
        assertEquals(100L, savedMember.getLeague().getId());
        assertEquals("OWNER", savedMember.getRole());

        assertEquals(1, response.memberCount());
        assertEquals(100L, response.id());
    }

    @Test
    void getLeagueReturnsResponseForMember() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 4, 3, null);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 11L))
                .thenReturn(Optional.of(newMember(11L, "MEMBER", league)));
        when(leagueMemberRepository.countByLeagueId(1L)).thenReturn(3);

        LeagueResponse response = service.getLeague(11L, 1L);

        assertEquals(1L, response.id());
        assertEquals(3, response.memberCount());
        assertEquals(LeagueStatus.INACTIVE.name(), response.status());
    }

    @Test
    void getLeagueRejectsNonMember() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 4, 1, null);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 11L)).thenReturn(Optional.empty());

        assertThrows(LeagueAccessDeniedException.class, () -> service.getLeague(11L, 1L));
    }

    @Test
    void getLeagueThrowsWhenLeagueMissing() {
        when(leagueRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(LeagueNotFoundException.class, () -> service.getLeague(10L, 1L));
    }

    @Test
    void listUserLeaguesReturnsMemberLeagues() {
        League league1 = league(1L, 10L, LeagueStatus.INACTIVE, 4, 2, null);
        League league2 = league(2L, 20L, LeagueStatus.ACTIVE, 5, 3, null);
        LeagueMember membership1 = newMember(30L, "MEMBER", league1);
        LeagueMember membership2 = newMember(30L, "MEMBER", league2);
        when(leagueMemberRepository.findByUserId(30L)).thenReturn(List.of(membership1, membership2));
        when(leagueRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(league1, league2));
        when(leagueMemberRepository.countByLeagueId(1L)).thenReturn(2);
        when(leagueMemberRepository.countByLeagueId(2L)).thenReturn(3);

        List<LeagueResponse> responses = service.listUserLeagues(30L);

        assertEquals(2, responses.size());
        assertEquals(List.of(1L, 2L), responses.stream().map(LeagueResponse::id).toList());
        assertEquals(2, responses.get(0).memberCount());
        assertEquals(3, responses.get(1).memberCount());
    }

    @Test
    void listUserLeaguesReturnsEmptyWhenNoMemberships() {
        when(leagueMemberRepository.findByUserId(9L)).thenReturn(List.of());
        when(leagueRepository.findAllById(List.of())).thenReturn(List.of());

        List<LeagueResponse> responses = service.listUserLeagues(9L);

        assertEquals(0, responses.size());
    }

    @Test
    void createInviteRequiresOwnerAndPersistsInvite() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 4, 1, null);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueInviteRepository.save(any(LeagueInvite.class))).thenAnswer(inv -> {
            LeagueInvite invite = inv.getArgument(0);
            invite.setId(300L);
            invite.setCreatedAt(OffsetDateTime.now());
            return invite;
        });

        CreateInviteRequest req = new CreateInviteRequest(OffsetDateTime.now().plusDays(1), 4);
        LeagueInviteResponse response = service.createInvite(10L, 1L, req);

        ArgumentCaptor<LeagueInvite> captor = ArgumentCaptor.forClass(LeagueInvite.class);
        verify(leagueInviteRepository).save(captor.capture());
        LeagueInvite saved = captor.getValue();

        assertEquals(300L, response.id());
        assertEquals(response.code(), saved.getCode());
        assertEquals(4, saved.getMaxUses());
        assertEquals(req.expiresAt(), saved.getExpiresAt());
        assertEquals(4, response.maxUses());
        assertEquals(1L, saved.getLeague().getId());
        assertEquals(10L, saved.getCreatedBy());
        assertNotNull(response.createdAt());
        assertEquals(12, saved.getCode().length());
        assertTrue(saved.getCode().matches("[A-Z0-9]+"));
    }

    @Test
    void createInviteRejectsNonOwner() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 4, 1, null);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));

        CreateInviteRequest req = new CreateInviteRequest(null, null);
        assertThrows(LeagueAccessDeniedException.class, () -> service.createInvite(11L, 1L, req));
    }

    @Test
    void joinLeagueSucceedsWithValidInvite() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 3, 1, null);
        LeagueInvite invite = invite(league, "VALIDINVITE12", 10, 0, OffsetDateTime.now().plusDays(1));

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueInviteRepository.findByCodeAndRevokedAtIsNull("VALIDINVITE12"))
                .thenReturn(Optional.of(invite));
        when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L)).thenReturn(Optional.empty());
        when(leagueMemberRepository.save(any(LeagueMember.class))).thenAnswer(inv -> {
            LeagueMember member = inv.getArgument(0);
            member.setId(400L);
            return member;
        });
        when(leagueInviteRepository.save(any(LeagueInvite.class))).thenAnswer(inv -> inv.getArgument(0));

        LeagueMemberResponse response = service.joinLeague(20L, 1L, new JoinLeagueRequest("VALIDINVITE12"));

        ArgumentCaptor<LeagueInvite> inviteCaptor = ArgumentCaptor.forClass(LeagueInvite.class);
        verify(leagueInviteRepository).save(inviteCaptor.capture());
        assertEquals(1, inviteCaptor.getValue().getUsesCount());
        assertEquals(LeagueStatus.INACTIVE.name(), invite.getLeague().getStatus().name());

        assertEquals(400L, response.id());
        assertEquals(20L, response.userId());
        assertEquals("MEMBER", response.role());
    }

    @Test
    void joinLeagueAllowsNullMaxUses() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 5, 1, null);
        LeagueInvite invite = invite(league, "OPENINVITE01", null, 2, OffsetDateTime.now().plusDays(1));

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueInviteRepository.findByCodeAndRevokedAtIsNull("OPENINVITE01"))
                .thenReturn(Optional.of(invite));
        when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L)).thenReturn(Optional.empty());
        when(leagueMemberRepository.save(any(LeagueMember.class))).thenAnswer(inv -> {
            LeagueMember member = inv.getArgument(0);
            member.setId(401L);
            return member;
        });
        when(leagueInviteRepository.save(any(LeagueInvite.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.joinLeague(20L, 1L, new JoinLeagueRequest("OPENINVITE01")));

        ArgumentCaptor<LeagueInvite> inviteCaptor = ArgumentCaptor.forClass(LeagueInvite.class);
        verify(leagueInviteRepository).save(inviteCaptor.capture());
        assertEquals(3, inviteCaptor.getValue().getUsesCount());
    }

    @Test
    void joinLeagueRejectsWrongLeagueInvite() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 5, 1, null);
        League otherLeague = league(2L, 11L, LeagueStatus.INACTIVE, 5, 1, null);
        LeagueInvite invite = invite(otherLeague, "OTHERLEAGUE", 10, 0, OffsetDateTime.now().plusDays(1));

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueInviteRepository.findByCodeAndRevokedAtIsNull("OTHERLEAGUE")).thenReturn(Optional.of(invite));

        assertThrows(InvalidInviteException.class, () -> service.joinLeague(20L, 1L, new JoinLeagueRequest("OTHERLEAGUE")));
    }

    @Test
    void joinLeagueRejectsExpiredInvite() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 5, 1, null);
        LeagueInvite invite = invite(league, "EXPIREDINV1", 10, 0, OffsetDateTime.now().minusMinutes(1));

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueInviteRepository.findByCodeAndRevokedAtIsNull("EXPIREDINV1")).thenReturn(Optional.of(invite));

        assertThrows(InvalidInviteException.class, () -> service.joinLeague(20L, 1L, new JoinLeagueRequest("EXPIREDINV1")));
    }

    @Test
    void joinLeagueRejectsWhenInviteCodeInvalid() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 5, 1, null);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueInviteRepository.findByCodeAndRevokedAtIsNull("MISSING")).thenReturn(Optional.empty());

        assertThrows(InvalidInviteException.class, () -> service.joinLeague(20L, 1L, new JoinLeagueRequest("MISSING")));
    }

    @Test
    void joinLeagueRejectsWhenInviteUsesExceeded() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 5, 1, null);
        LeagueInvite invite = invite(league, "FULLUSEINV01", 1, 1, OffsetDateTime.now().plusDays(1));

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueInviteRepository.findByCodeAndRevokedAtIsNull("FULLUSEINV01")).thenReturn(Optional.of(invite));
        when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L)).thenReturn(Optional.empty());

        assertThrows(InvalidInviteException.class, () -> service.joinLeague(20L, 1L, new JoinLeagueRequest("FULLUSEINV01")));
        verify(leagueMemberRepository, never()).save(any(LeagueMember.class));
    }

    @Test
    void joinLeagueRejectsIfLeagueAlreadyStarted() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 5, 1, OffsetDateTime.now().minusMinutes(1));
        LeagueInvite invite = invite(league, "STARTEDCODE", 10, 0, OffsetDateTime.now().plusDays(1));

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueInviteRepository.findByCodeAndRevokedAtIsNull("STARTEDCODE")).thenReturn(Optional.of(invite));
        when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L)).thenReturn(Optional.empty());

        assertThrows(LeagueStateException.class, () -> service.joinLeague(20L, 1L, new JoinLeagueRequest("STARTEDCODE")));
    }

    @Test
    void joinLeagueRejectsIfLeagueIsFull() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 1, 1, null);
        LeagueInvite invite = invite(league, "FULLLEAGUE1", 10, 0, OffsetDateTime.now().plusDays(1));

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueInviteRepository.findByCodeAndRevokedAtIsNull("FULLLEAGUE1")).thenReturn(Optional.of(invite));
        when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L)).thenReturn(Optional.empty());

        assertThrows(LeagueStateException.class, () -> service.joinLeague(20L, 1L, new JoinLeagueRequest("FULLLEAGUE1")));
    }

    @Test
    void joinLeagueRejectsIfAlreadyMember() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 5, 1, null);
        LeagueInvite invite = invite(league, "ALREADYMEM01", 10, 0, OffsetDateTime.now().plusDays(1));

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueInviteRepository.findByCodeAndRevokedAtIsNull("ALREADYMEM01")).thenReturn(Optional.of(invite));
        when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L))
                .thenReturn(Optional.of(newMember(20L, "MEMBER", league)));

        assertThrows(LeagueStateException.class, () -> service.joinLeague(20L, 1L, new JoinLeagueRequest("ALREADYMEM01")));
        verify(leagueMemberRepository, never()).save(any(LeagueMember.class));
    }

    @Test
    void startLeagueStartsInactiveLeague() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 3, 1, null);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueRepository.save(any(League.class))).thenAnswer(inv -> inv.getArgument(0));
        when(leagueMemberRepository.countByLeagueId(1L)).thenReturn(1);

        LeagueResponse response = service.startLeague(10L, 1L);

        ArgumentCaptor<League> captor = ArgumentCaptor.forClass(League.class);
        verify(leagueRepository).save(captor.capture());
        League saved = captor.getValue();

        assertEquals(LeagueStatus.ACTIVE, saved.getStatus());
        assertNotNull(saved.getStartedAt());
        assertNotNull(saved.getInitialStipendIssuedAt());
        assertEquals(1, response.memberCount());
        assertEquals(LeagueStatus.ACTIVE.name(), response.status());
    }

    @Test
    void startLeagueRejectsNonOwner() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 3, 1, null);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));

        assertThrows(LeagueAccessDeniedException.class, () -> service.startLeague(11L, 1L));
    }

    @Test
    void startLeagueRejectsIfNotInactive() {
        League league = league(1L, 10L, LeagueStatus.ACTIVE, 3, 1, OffsetDateTime.now().minusMinutes(1));
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));

        assertThrows(LeagueStateException.class, () -> service.startLeague(10L, 1L));
    }

    @Test
    void removeMemberDeletesTargetMember() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 4, 2, null);
        LeagueMember target = newMember(20L, "MEMBER", league);

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L)).thenReturn(Optional.of(target));

        service.removeMember(10L, 1L, 20L);

        verify(leagueMemberRepository).delete(target);
    }

    @Test
    void removeMemberRejectsSelfRemoval() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 4, 1, null);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));

        assertThrows(LeagueStateException.class, () -> service.removeMember(10L, 1L, 10L));
    }

    @Test
    void removeMemberRejectsIfStarted() {
        League league = league(1L, 10L, LeagueStatus.ACTIVE, 4, 1, OffsetDateTime.now().minusMinutes(1));
        LeagueMember target = newMember(20L, "MEMBER", league);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L)).thenReturn(Optional.of(target));

        assertThrows(LeagueStateException.class, () -> service.removeMember(10L, 1L, 20L));
    }

    @Test
    void removeMemberRejectsNonOwner() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 4, 2, null);
        LeagueMember target = newMember(20L, "MEMBER", league);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L)).thenReturn(Optional.of(target));

        assertThrows(LeagueAccessDeniedException.class, () -> service.removeMember(11L, 1L, 20L));
    }

    @Test
    void leaveLeagueDeletesNonOwnerMember() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 4, 2, null);
        LeagueMember member = newMember(20L, "MEMBER", league);

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L)).thenReturn(Optional.of(member));

        service.leaveLeague(20L, 1L);

        verify(leagueMemberRepository).delete(member);
    }

    @Test
    void leaveLeagueRejectsOwner() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 4, 1, null);
        LeagueMember owner = newMember(10L, "OWNER", league);

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 10L)).thenReturn(Optional.of(owner));

        assertThrows(LeagueStateException.class, () -> service.leaveLeague(10L, 1L));
    }

    @Test
    void leaveLeagueRejectsIfLeagueStarted() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 4, 2, OffsetDateTime.now().minusMinutes(1));
        LeagueMember member = newMember(20L, "MEMBER", league);

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L)).thenReturn(Optional.of(member));

        assertThrows(LeagueStateException.class, () -> service.leaveLeague(20L, 1L));
    }

    @Test
    void leaveLeagueRejectsIfNotMember() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 4, 1, null);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L)).thenReturn(Optional.empty());

        assertThrows(LeagueNotFoundException.class, () -> service.leaveLeague(20L, 1L));
    }

    @Test
    void listMembersReturnsMembersForMember() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 4, 2, null);
        LeagueMember caller = newMember(20L, "MEMBER", league);
        LeagueMember owner = newMember(10L, "OWNER", league);
        LeagueMember member = newMember(21L, "MEMBER", league);

        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L)).thenReturn(Optional.of(caller));
        when(leagueMemberRepository.findAllByLeagueId(1L)).thenReturn(List.of(owner, caller, member));

        List<LeagueMemberResponse> response = service.listMembers(20L, 1L);

        assertEquals(3, response.size());
        assertEquals(List.of(10L, 20L, 21L), response.stream().map(LeagueMemberResponse::userId).toList());
    }

    @Test
    void listMembersRejectsNonMember() {
        League league = league(1L, 10L, LeagueStatus.INACTIVE, 4, 2, null);
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L)).thenReturn(Optional.empty());

        assertThrows(LeagueAccessDeniedException.class, () -> service.listMembers(20L, 1L));
        verify(leagueMemberRepository, never()).findAllByLeagueId(any(Long.class));
    }

    @Test
    void listMembersThrowsWhenLeagueMissing() {
        when(leagueRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(LeagueNotFoundException.class, () -> service.listMembers(20L, 1L));
    }

    private CreateLeagueRequest createLeagueRequest() {
        return new CreateLeagueRequest(
                "Test League",
                4,
                OffsetDateTime.now().plusDays(1),
                OffsetDateTime.now().plusMonths(1),
                STIPEND,
                WEEKLY_STIPEND,
                (short) 1
        );
    }

    private League league(Long id, Long ownerUserId, LeagueStatus status, int maxMembers, int currentMembers, OffsetDateTime startedAt) {
        League league = new League();
        league.setId(id);
        league.setOwnerUserId(ownerUserId);
        league.setName("League " + id);
        league.setStatus(status);
        league.setMaxMembers(maxMembers);
        league.setSeasonStartAt(OffsetDateTime.now().plusDays(1));
        league.setSeasonEndAt(OffsetDateTime.now().plusMonths(1));
        league.setInitialStipendAmount(STIPEND);
        league.setWeeklyStipendAmount(WEEKLY_STIPEND);
        league.setWeeklyPayoutDowUtc((short) 1);
        league.setStartedAt(startedAt);

        List<LeagueMember> members = new ArrayList<>();
        for (int i = 0; i < currentMembers; i++) {
            LeagueMember member = new LeagueMember();
            member.setUserId(ownerUserId + i);
            member.setRole(i == 0 ? "OWNER" : "MEMBER");
            members.add(member);
        }
        league.setMembers(members);
        return league;
    }

    private LeagueMember newMember(Long userId, String role, League league) {
        LeagueMember member = new LeagueMember();
        member.setUserId(userId);
        member.setRole(role);
        member.setLeague(league);
        member.setId(userId);
        return member;
    }

    private LeagueInvite invite(League league, String code, Integer maxUses, int usesCount, OffsetDateTime expiresAt) {
        LeagueInvite invite = new LeagueInvite();
        invite.setLeague(league);
        invite.setCode(code);
        invite.setCreatedBy(99L);
        invite.setMaxUses(maxUses);
        invite.setUsesCount(usesCount);
        invite.setExpiresAt(expiresAt);
        invite.setRevokedAt(null);
        return invite;
    }
}
