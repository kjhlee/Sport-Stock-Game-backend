package com.sportstock.league.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import com.sportstock.common.dto.league.CreateInviteRequest;
import com.sportstock.common.dto.league.CreateLeagueRequest;
import com.sportstock.common.dto.league.JoinLeagueRequest;
import com.sportstock.common.dto.league.LeagueInviteResponse;
import com.sportstock.common.dto.league.LeagueMemberResponse;
import com.sportstock.common.dto.league.LeagueResponse;
import com.sportstock.common.enums.league.InitialStipendStatus;
import com.sportstock.common.enums.league.LeagueStatus;
import com.sportstock.league.client.IngestionServiceClient;
import com.sportstock.league.client.TransactionServiceClient;
import com.sportstock.league.entity.League;
import com.sportstock.league.entity.LeagueInvite;
import com.sportstock.league.entity.LeagueMember;
import com.sportstock.league.exception.InvalidInviteException;
import com.sportstock.league.exception.LeagueAccessDeniedException;
import com.sportstock.league.exception.LeagueNotFoundException;
import com.sportstock.league.exception.LeagueStateException;
import com.sportstock.league.repo.LeagueInviteRepository;
import com.sportstock.league.repo.LeagueMemberRepository;
import com.sportstock.league.repo.LeagueRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeagueService Comprehensive Tests")
class LeagueServiceComprehensiveTest {

  @Mock private LeagueRepository leagueRepository;
  @Mock private LeagueMemberRepository leagueMemberRepository;
  @Mock private LeagueInviteRepository leagueInviteRepository;
  @Mock private TransactionServiceClient transactionServiceClient;
  @Mock private IngestionServiceClient ingestionServiceClient;

  private LeagueService service;

  @BeforeEach
  void setUp() {
    service =
        new LeagueService(
            leagueRepository,
            leagueMemberRepository,
            leagueInviteRepository,
            transactionServiceClient,
            ingestionServiceClient);

    lenient()
        .when(leagueRepository.save(any(League.class)))
        .thenAnswer(
            invocation -> {
              League league = invocation.getArgument(0);
              if (league.getId() == null) {
                league.setId(1L);
              }
              return league;
            });
    lenient()
        .when(leagueMemberRepository.save(any(LeagueMember.class)))
        .thenAnswer(
            invocation -> {
              LeagueMember member = invocation.getArgument(0);
              if (member.getId() == null) {
                member.setId(1L);
              }
              return member;
            });
    lenient()
        .when(leagueInviteRepository.save(any(LeagueInvite.class)))
        .thenAnswer(
            invocation -> {
              LeagueInvite invite = invocation.getArgument(0);
              if (invite.getId() == null) {
                invite.setId(1L);
              }
              return invite;
            });
  }

  @Nested
  @DisplayName("Create League Tests")
  class CreateLeagueTests {

    @Test
    @DisplayName("Should create league with owner as first member")
    void createLeague_success() {
      CreateLeagueRequest request =
          new CreateLeagueRequest(
              "Test League",
              10,
              OffsetDateTime.now().plusDays(7),
              OffsetDateTime.now().plusDays(90),
              bd("1000.00"),
              bd("100.00"));

      LeagueResponse response = service.createLeague(100L, request);

      assertNotNull(response);
      assertEquals("Test League", response.name());
      assertEquals(1, response.memberCount());

      ArgumentCaptor<League> leagueCaptor = ArgumentCaptor.forClass(League.class);
      ArgumentCaptor<LeagueMember> memberCaptor = ArgumentCaptor.forClass(LeagueMember.class);

      verify(leagueRepository).save(leagueCaptor.capture());
      verify(leagueMemberRepository).save(memberCaptor.capture());

      League savedLeague = leagueCaptor.getValue();
      assertEquals(100L, savedLeague.getOwnerUserId());
      assertEquals(LeagueStatus.INACTIVE, savedLeague.getStatus());
      assertEquals(10, savedLeague.getMaxMembers());

      LeagueMember savedMember = memberCaptor.getValue();
      assertEquals(100L, savedMember.getUserId());
      assertEquals("OWNER", savedMember.getRole());
    }
  }

  @Nested
  @DisplayName("Get League Tests")
  class GetLeagueTests {

    @Test
    @DisplayName("Should return league for member")
    void getLeague_asMember_success() {
      League league = createTestLeague(1L, 10L);
      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
      when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L))
          .thenReturn(Optional.of(createMember(20L, league, "MEMBER")));
      when(leagueMemberRepository.countByLeagueId(1L)).thenReturn(5);

      LeagueResponse response = service.getLeague(20L, 1L);

      assertNotNull(response);
      assertEquals("Test League", response.name());
      assertEquals(5, response.memberCount());
    }

    @Test
    @DisplayName("Should throw exception when league not found")
    void getLeague_notFound() {
      when(leagueRepository.findById(999L)).thenReturn(Optional.empty());

      assertThrows(LeagueNotFoundException.class, () -> service.getLeague(10L, 999L));
    }

    @Test
    @DisplayName("Should throw exception when user is not a member")
    void getLeague_notMember() {
      League league = createTestLeague(1L, 10L);
      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
      when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 30L)).thenReturn(Optional.empty());

      assertThrows(LeagueAccessDeniedException.class, () -> service.getLeague(30L, 1L));
    }
  }

  @Nested
  @DisplayName("List User Leagues Tests")
  class ListUserLeaguesTests {

    @Test
    @DisplayName("Should return paginated list of user's leagues")
    void listUserLeagues_success() {
      League league1 = createTestLeague(1L, 10L);
      League league2 = createTestLeague(2L, 10L);

      LeagueMember member1 = createMember(10L, league1, "OWNER");
      LeagueMember member2 = createMember(10L, league2, "MEMBER");

      Page<LeagueMember> memberPage = new PageImpl<>(Arrays.asList(member1, member2));

      when(leagueMemberRepository.findByUserId(eq(10L), any(PageRequest.class)))
          .thenReturn(memberPage);
      when(leagueRepository.findAllById(Arrays.asList(1L, 2L)))
          .thenReturn(Arrays.asList(league1, league2));
      when(leagueMemberRepository.countByLeagueIds(Arrays.asList(1L, 2L)))
          .thenReturn(Arrays.asList(new Object[] {1L, 3L}, new Object[] {2L, 5L}));

      Page<LeagueResponse> result = service.listUserLeagues(10L, PageRequest.of(0, 10));

      assertNotNull(result);
      assertEquals(2, result.getContent().size());
    }

    @Test
    @DisplayName("Should return empty page when user has no leagues")
    void listUserLeagues_empty() {
      when(leagueMemberRepository.findByUserId(eq(10L), any(PageRequest.class)))
          .thenReturn(Page.empty());

      Page<LeagueResponse> result = service.listUserLeagues(10L, PageRequest.of(0, 10));

      assertNotNull(result);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("Create Invite Tests")
  class CreateInviteTests {

    @Test
    @DisplayName("Should create invite when owner and league is inactive")
    void createInvite_success() {
      League league = createTestLeague(1L, 10L);
      CreateInviteRequest request = new CreateInviteRequest(OffsetDateTime.now().plusDays(7), 10);

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
      when(leagueInviteRepository.findByCode(anyString())).thenReturn(Optional.empty());

      LeagueInviteResponse response = service.createInvite(10L, 1L, request);

      assertNotNull(response);
      assertNotNull(response.code());
      assertEquals(12, response.code().length());

      verify(leagueInviteRepository).save(any(LeagueInvite.class));
    }

    @Test
    @DisplayName("Should throw exception when non-owner tries to create invite")
    void createInvite_notOwner() {
      League league = createTestLeague(1L, 10L);
      CreateInviteRequest request = new CreateInviteRequest(OffsetDateTime.now().plusDays(7), 10);

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));

      assertThrows(LeagueAccessDeniedException.class, () -> service.createInvite(20L, 1L, request));
    }

    @Test
    @DisplayName("Should throw exception when league is not inactive")
    void createInvite_leagueActive() {
      League league = createTestLeague(1L, 10L);
      league.setStatus(LeagueStatus.ACTIVE);
      CreateInviteRequest request = new CreateInviteRequest(OffsetDateTime.now().plusDays(7), 10);

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));

      assertThrows(LeagueStateException.class, () -> service.createInvite(10L, 1L, request));
    }
  }

  @Nested
  @DisplayName("Join League Tests")
  class JoinLeagueTests {

    @Test
    @DisplayName("Should join league with valid invite")
    void joinLeague_success() {
      League league = createTestLeague(1L, 10L);
      LeagueInvite invite = createInvite("ABC123", league, 5, 0);

      when(leagueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(league));
      when(leagueInviteRepository.findByCodeAndRevokedAtIsNull("ABC123"))
          .thenReturn(Optional.of(invite));
      when(leagueMemberRepository.countByLeagueId(1L)).thenReturn(2);
      when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L)).thenReturn(Optional.empty());
      when(leagueInviteRepository.incrementUsesCount(invite.getId())).thenReturn(1);

      LeagueMemberResponse response = service.joinLeague(20L, 1L, new JoinLeagueRequest("ABC123"));

      assertNotNull(response);
      assertEquals(20L, response.userId());
      assertEquals("MEMBER", response.role());

      verify(leagueMemberRepository).save(any(LeagueMember.class));
      verify(leagueInviteRepository).incrementUsesCount(anyLong());
    }

    @Test
    @DisplayName("Should throw exception for invalid invite code")
    void joinLeague_invalidCode() {
      when(leagueRepository.findByIdForUpdate(1L))
          .thenReturn(Optional.of(createTestLeague(1L, 10L)));
      when(leagueInviteRepository.findByCodeAndRevokedAtIsNull("INVALID"))
          .thenReturn(Optional.empty());

      assertThrows(
          InvalidInviteException.class,
          () -> service.joinLeague(20L, 1L, new JoinLeagueRequest("INVALID")));
    }

    @Test
    @DisplayName("Should throw exception when invite code doesn't match league")
    void joinLeague_wrongLeague() {
      League league1 = createTestLeague(1L, 10L);
      League league2 = createTestLeague(2L, 20L);
      LeagueInvite invite = createInvite("ABC123", league2, 5, 0);

      when(leagueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(league1));
      when(leagueInviteRepository.findByCodeAndRevokedAtIsNull("ABC123"))
          .thenReturn(Optional.of(invite));

      assertThrows(
          InvalidInviteException.class,
          () -> service.joinLeague(20L, 1L, new JoinLeagueRequest("ABC123")));
    }

    @Test
    @DisplayName("Should throw exception when invite has expired")
    void joinLeague_expiredInvite() {
      League league = createTestLeague(1L, 10L);
      LeagueInvite invite = createInvite("ABC123", league, 5, 0);
      invite.setExpiresAt(OffsetDateTime.now().minusDays(1));

      when(leagueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(league));
      when(leagueInviteRepository.findByCodeAndRevokedAtIsNull("ABC123"))
          .thenReturn(Optional.of(invite));

      assertThrows(
          InvalidInviteException.class,
          () -> service.joinLeague(20L, 1L, new JoinLeagueRequest("ABC123")));
    }

    @Test
    @DisplayName("Should throw exception when league has already started")
    void joinLeague_leagueStarted() {
      League league = createTestLeague(1L, 10L);
      league.setStatus(LeagueStatus.ACTIVE);
      league.setStartedAt(OffsetDateTime.now().minusDays(1));
      LeagueInvite invite = createInvite("ABC123", league, 5, 0);

      when(leagueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(league));
      when(leagueInviteRepository.findByCodeAndRevokedAtIsNull("ABC123"))
          .thenReturn(Optional.of(invite));

      assertThrows(
          LeagueStateException.class,
          () -> service.joinLeague(20L, 1L, new JoinLeagueRequest("ABC123")));
    }

    @Test
    @DisplayName("Should throw exception when league is full")
    void joinLeague_leagueFull() {
      League league = createTestLeague(1L, 10L);
      league.setMaxMembers(5);
      LeagueInvite invite = createInvite("ABC123", league, 10, 0);

      when(leagueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(league));
      when(leagueInviteRepository.findByCodeAndRevokedAtIsNull("ABC123"))
          .thenReturn(Optional.of(invite));
      when(leagueMemberRepository.countByLeagueId(1L)).thenReturn(5);

      assertThrows(
          LeagueStateException.class,
          () -> service.joinLeague(20L, 1L, new JoinLeagueRequest("ABC123")));
    }

    @Test
    @DisplayName("Should throw exception when user is already a member")
    void joinLeague_alreadyMember() {
      League league = createTestLeague(1L, 10L);
      LeagueInvite invite = createInvite("ABC123", league, 5, 0);
      LeagueMember existingMember = createMember(20L, league, "MEMBER");

      when(leagueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(league));
      when(leagueInviteRepository.findByCodeAndRevokedAtIsNull("ABC123"))
          .thenReturn(Optional.of(invite));
      when(leagueMemberRepository.countByLeagueId(1L)).thenReturn(2);
      when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L))
          .thenReturn(Optional.of(existingMember));

      assertThrows(
          LeagueStateException.class,
          () -> service.joinLeague(20L, 1L, new JoinLeagueRequest("ABC123")));
    }
  }

  @Nested
  @DisplayName("Start League Tests")
  class StartLeagueTests {

    @Test
    @DisplayName("Should start league during preseason and keep stipend pending")
    void startLeague_preseason() {
      League league = createTestLeague(1L, 10L);

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
      when(leagueMemberRepository.countByLeagueId(1L)).thenReturn(3);
      when(ingestionServiceClient.getCurrentWeekOrPreseasonOptional())
          .thenReturn(
              new CurrentWeekResponse(
                  2026, "1", "Preseason", 2, "Preseason Week 2", Instant.now(), Instant.now()));

      LeagueResponse response = service.startLeague(10L, 1L);

      assertEquals(LeagueStatus.ACTIVE, league.getStatus());
      assertEquals(InitialStipendStatus.PENDING, league.getInitialStipendStatus());
      assertNotNull(league.getStartedAt());
      assertNull(league.getInitialStipendIssuedAt());
      verify(transactionServiceClient, never()).issueInitialStipends(anyLong(), any());
    }

    @Test
    @DisplayName("Should start league during regular season and issue stipends immediately")
    void startLeague_regularSeason() {
      League league = createTestLeague(1L, 10L);

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
      when(leagueMemberRepository.countByLeagueId(1L)).thenReturn(4);
      when(ingestionServiceClient.getCurrentWeekOrPreseasonOptional())
          .thenReturn(
              new CurrentWeekResponse(
                  2026, "2", "Regular", 5, "Week 5", Instant.now(), Instant.now()));

      LeagueResponse response = service.startLeague(10L, 1L);

      assertEquals(LeagueStatus.ACTIVE, league.getStatus());
      assertEquals(InitialStipendStatus.ISSUED, league.getInitialStipendStatus());
      assertNotNull(league.getStartedAt());
      assertNotNull(league.getInitialStipendIssuedAt());
      verify(transactionServiceClient).issueInitialStipends(1L, bd("1000.00"));
    }

    @Test
    @DisplayName("Should throw exception when non-owner tries to start league")
    void startLeague_notOwner() {
      League league = createTestLeague(1L, 10L);
      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));

      assertThrows(LeagueAccessDeniedException.class, () -> service.startLeague(20L, 1L));
    }

    @Test
    @DisplayName("Should throw exception when league is already active")
    void startLeague_alreadyActive() {
      League league = createTestLeague(1L, 10L);
      league.setStatus(LeagueStatus.ACTIVE);

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));

      assertThrows(LeagueStateException.class, () -> service.startLeague(10L, 1L));
    }

    @Test
    @DisplayName("Should throw exception when league has less than 2 members")
    void startLeague_notEnoughMembers() {
      League league = createTestLeague(1L, 10L);

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
      when(leagueMemberRepository.countByLeagueId(1L)).thenReturn(1);

      assertThrows(LeagueStateException.class, () -> service.startLeague(10L, 1L));
    }

    @Test
    @DisplayName("Should throw exception during NFL offseason")
    void startLeague_offseason() {
      League league = createTestLeague(1L, 10L);

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
      when(leagueMemberRepository.countByLeagueId(1L)).thenReturn(3);
      when(ingestionServiceClient.getCurrentWeekOrPreseasonOptional()).thenReturn(null);

      assertThrows(LeagueStateException.class, () -> service.startLeague(10L, 1L));
    }

    @Test
    @DisplayName("Should throw exception during postseason")
    void startLeague_postseason() {
      League league = createTestLeague(1L, 10L);

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
      when(leagueMemberRepository.countByLeagueId(1L)).thenReturn(3);
      when(ingestionServiceClient.getCurrentWeekOrPreseasonOptional())
          .thenReturn(
              new CurrentWeekResponse(
                  2026, "3", "Postseason", 1, "Wild Card", Instant.now(), Instant.now()));

      assertThrows(LeagueStateException.class, () -> service.startLeague(10L, 1L));
    }
  }

  @Nested
  @DisplayName("Remove Member Tests")
  class RemoveMemberTests {

    @Test
    @DisplayName("Should remove member when owner and league is inactive")
    void removeMember_success() {
      League league = createTestLeague(1L, 10L);
      LeagueMember member = createMember(20L, league, "MEMBER");

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
      when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L)).thenReturn(Optional.of(member));

      assertDoesNotThrow(() -> service.removeMember(10L, 1L, 20L));

      verify(leagueMemberRepository).delete(member);
    }

    @Test
    @DisplayName("Should throw exception when trying to remove self")
    void removeMember_cannotRemoveSelf() {
      League league = createTestLeague(1L, 10L);
      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));

      assertThrows(LeagueStateException.class, () -> service.removeMember(10L, 1L, 10L));
    }

    @Test
    @DisplayName("Should throw exception when league has started")
    void removeMember_leagueStarted() {
      League league = createTestLeague(1L, 10L);
      league.setStatus(LeagueStatus.ACTIVE);
      league.setStartedAt(OffsetDateTime.now().minusDays(1));

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));

      assertThrows(LeagueStateException.class, () -> service.removeMember(10L, 1L, 20L));
    }
  }

  @Nested
  @DisplayName("Leave League Tests")
  class LeaveLeagueTests {

    @Test
    @DisplayName("Should allow member to leave inactive league")
    void leaveLeague_success() {
      League league = createTestLeague(1L, 10L);
      LeagueMember member = createMember(20L, league, "MEMBER");

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
      when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L)).thenReturn(Optional.of(member));

      assertDoesNotThrow(() -> service.leaveLeague(20L, 1L));

      verify(leagueMemberRepository).delete(member);
    }

    @Test
    @DisplayName("Should throw exception when owner tries to leave")
    void leaveLeague_ownerCannotLeave() {
      League league = createTestLeague(1L, 10L);
      LeagueMember ownerMember = createMember(10L, league, "OWNER");

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
      when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 10L))
          .thenReturn(Optional.of(ownerMember));

      assertThrows(LeagueStateException.class, () -> service.leaveLeague(10L, 1L));
    }

    @Test
    @DisplayName("Should throw exception when league has started")
    void leaveLeague_leagueStarted() {
      League league = createTestLeague(1L, 10L);
      league.setStatus(LeagueStatus.ACTIVE);
      league.setStartedAt(OffsetDateTime.now());
      LeagueMember member = createMember(20L, league, "MEMBER");

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
      when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L)).thenReturn(Optional.of(member));

      assertThrows(LeagueStateException.class, () -> service.leaveLeague(20L, 1L));
    }

    @Test
    @DisplayName("Should throw archived-specific exception when league is archived")
    void leaveLeague_archivedLeague() {
      League league = createTestLeague(1L, 10L);
      league.setStatus(LeagueStatus.ARCHIVED);
      LeagueMember member = createMember(20L, league, "MEMBER");

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
      when(leagueMemberRepository.findByLeagueIdAndUserId(1L, 20L)).thenReturn(Optional.of(member));

      LeagueStateException ex =
          assertThrows(LeagueStateException.class, () -> service.leaveLeague(20L, 1L));

      assertEquals("Cannot leave an archived league", ex.getMessage());
    }
  }

  @Nested
  @DisplayName("Revoke Invite Tests")
  class RevokeInviteTests {

    @Test
    @DisplayName("Should revoke invite when owner")
    void revokeInvite_success() {
      League league = createTestLeague(1L, 10L);
      LeagueInvite invite = createInvite("ABC123", league, 5, 1);
      invite.setId(100L);

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
      when(leagueInviteRepository.findById(100L)).thenReturn(Optional.of(invite));

      assertDoesNotThrow(() -> service.revokeInvite(10L, 1L, 100L));

      assertNotNull(invite.getRevokedAt());
      verify(leagueInviteRepository).save(invite);
    }

    @Test
    @DisplayName("Should throw exception when invite already revoked")
    void revokeInvite_alreadyRevoked() {
      League league = createTestLeague(1L, 10L);
      LeagueInvite invite = createInvite("ABC123", league, 5, 1);
      invite.setId(100L);
      invite.setRevokedAt(OffsetDateTime.now().minusDays(1));

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
      when(leagueInviteRepository.findById(100L)).thenReturn(Optional.of(invite));

      assertThrows(LeagueStateException.class, () -> service.revokeInvite(10L, 1L, 100L));
    }

    @Test
    @DisplayName("Should throw exception when invite doesn't belong to league")
    void revokeInvite_wrongLeague() {
      League league1 = createTestLeague(1L, 10L);
      League league2 = createTestLeague(2L, 20L);
      LeagueInvite invite = createInvite("ABC123", league2, 5, 1);
      invite.setId(100L);

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league1));
      when(leagueInviteRepository.findById(100L)).thenReturn(Optional.of(invite));

      assertThrows(LeagueAccessDeniedException.class, () -> service.revokeInvite(10L, 1L, 100L));
    }
  }

  @Nested
  @DisplayName("Archive League Tests")
  class ArchiveLeagueTests {

    @Test
    @DisplayName("Should archive active league when owner")
    void archiveLeague_success() {
      League league = createTestLeague(1L, 10L);
      league.setStatus(LeagueStatus.ACTIVE);

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
      when(leagueMemberRepository.countByLeagueId(1L)).thenReturn(5);

      LeagueResponse response = service.archiveLeague(10L, 1L);

      assertEquals(LeagueStatus.ARCHIVED, league.getStatus());
      verify(leagueRepository).save(league);
    }

    @Test
    @DisplayName("Should throw exception when league is already archived")
    void archiveLeague_alreadyArchived() {
      League league = createTestLeague(1L, 10L);
      league.setStatus(LeagueStatus.ARCHIVED);

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));

      assertThrows(LeagueStateException.class, () -> service.archiveLeague(10L, 1L));
    }
  }

  @Nested
  @DisplayName("Internal API Tests")
  class InternalApiTests {

    @Test
    @DisplayName("Should get member user IDs")
    void getMemberUserIds_success() {
      League league = createTestLeague(1L, 10L);
      List<LeagueMember> members =
          Arrays.asList(
              createMember(10L, league, "OWNER"),
              createMember(20L, league, "MEMBER"),
              createMember(30L, league, "MEMBER"));

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));
      when(leagueMemberRepository.findAllByLeagueId(1L)).thenReturn(members);

      List<Long> userIds = service.getMemberUserIds(1L);

      assertEquals(3, userIds.size());
      assertTrue(userIds.containsAll(Arrays.asList(10L, 20L, 30L)));
    }

    @Test
    @DisplayName("Should get leagues with pending stipend")
    void getLeaguesWithPendingStipend_success() {
      League league1 = createTestLeague(1L, 10L);
      league1.setStatus(LeagueStatus.ACTIVE);
      league1.setInitialStipendStatus(InitialStipendStatus.PENDING);

      League league2 = createTestLeague(2L, 20L);
      league2.setStatus(LeagueStatus.ACTIVE);
      league2.setInitialStipendStatus(InitialStipendStatus.PENDING);

      when(leagueRepository.findByStatusAndInitialStipendStatus(
              LeagueStatus.ACTIVE, InitialStipendStatus.PENDING))
          .thenReturn(Arrays.asList(league1, league2));
      when(leagueMemberRepository.countByLeagueId(1L)).thenReturn(3);
      when(leagueMemberRepository.countByLeagueId(2L)).thenReturn(5);

      List<LeagueResponse> leagues = service.getLeaguesWithPendingStipend();

      assertEquals(2, leagues.size());
    }

    @Test
    @DisplayName("Should update initial stipend status")
    void updateInitialStipendStatus_success() {
      League league = createTestLeague(1L, 10L);
      league.setInitialStipendStatus(InitialStipendStatus.PENDING);

      when(leagueRepository.findById(1L)).thenReturn(Optional.of(league));

      assertDoesNotThrow(() -> service.updateInitialStipendStatus(1L, "ISSUED"));

      assertEquals(InitialStipendStatus.ISSUED, league.getInitialStipendStatus());
      verify(leagueRepository).save(league);
    }

    @Test
    @DisplayName("Should get active leagues")
    void getActiveLeagues_success() {
      League league1 = createTestLeague(1L, 10L);
      league1.setStatus(LeagueStatus.ACTIVE);

      League league2 = createTestLeague(2L, 20L);
      league2.setStatus(LeagueStatus.ACTIVE);

      when(leagueRepository.findByStatus(LeagueStatus.ACTIVE))
          .thenReturn(Arrays.asList(league1, league2));
      when(leagueMemberRepository.countByLeagueId(anyLong())).thenReturn(3);

      List<LeagueResponse> leagues = service.getActiveLeagues();

      assertEquals(2, leagues.size());
    }
  }

  // Helper methods
  private League createTestLeague(Long id, Long ownerId) {
    League league = new League();
    league.setId(id);
    league.setOwnerUserId(ownerId);
    league.setName("Test League");
    league.setStatus(LeagueStatus.INACTIVE);
    league.setMaxMembers(10);
    league.setSeasonStartAt(OffsetDateTime.now().plusDays(7));
    league.setSeasonEndAt(OffsetDateTime.now().plusDays(90));
    league.setInitialStipendAmount(bd("1000.00"));
    league.setWeeklyStipendAmount(bd("100.00"));
    league.setInitialStipendStatus(InitialStipendStatus.NOT_APPLICABLE);
    league.setCreatedAt(OffsetDateTime.now());
    league.setUpdatedAt(OffsetDateTime.now());
    return league;
  }

  private LeagueMember createMember(Long userId, League league, String role) {
    LeagueMember member = new LeagueMember();
    member.setId(userId);
    member.setUserId(userId);
    member.setLeague(league);
    member.setRole(role);
    member.setJoinedAt(OffsetDateTime.now());
    member.setCreatedAt(OffsetDateTime.now());
    member.setUpdatedAt(OffsetDateTime.now());
    return member;
  }

  private LeagueInvite createInvite(
      String code, League league, Integer maxUses, Integer usesCount) {
    LeagueInvite invite = new LeagueInvite();
    invite.setId(1L); // Set ID for the invite
    invite.setCode(code);
    invite.setLeague(league);
    invite.setCreatedBy(league.getOwnerUserId());
    invite.setMaxUses(maxUses);
    invite.setUsesCount(usesCount);
    invite.setExpiresAt(OffsetDateTime.now().plusDays(7));
    invite.setCreatedAt(OffsetDateTime.now());
    invite.setUpdatedAt(OffsetDateTime.now());
    return invite;
  }

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }
}
