package com.sportstock.league.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import com.sportstock.common.dto.league.JoinLeagueRequest;
import com.sportstock.common.enums.league.InitialStipendStatus;
import com.sportstock.common.enums.league.LeagueStatus;
import com.sportstock.league.client.IngestionServiceClient;
import com.sportstock.league.client.TransactionServiceClient;
import com.sportstock.league.entity.League;
import com.sportstock.league.entity.LeagueInvite;
import com.sportstock.league.entity.LeagueMember;
import com.sportstock.league.repo.LeagueInviteRepository;
import com.sportstock.league.repo.LeagueMemberRepository;
import com.sportstock.league.repo.LeagueRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeagueServiceTest {

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
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient()
        .when(leagueMemberRepository.save(any(LeagueMember.class)))
        .thenAnswer(
            invocation -> {
              LeagueMember member = invocation.getArgument(0);
              member.setId(77L);
              return member;
            });
  }

  @Test
  void startLeague_duringPreseason_keepsInitialStipendPending() {
    League league = inactiveLeague(55L, 10L);

    when(leagueRepository.findById(55L)).thenReturn(Optional.of(league));
    when(leagueMemberRepository.countByLeagueId(55L)).thenReturn(2);
    when(ingestionServiceClient.getCurrentWeekOrPreseasonOptional())
        .thenReturn(
            new CurrentWeekResponse(
                2026, "1", "Preseason", 1, "Preseason Week 1", Instant.now(), Instant.now()));

    service.startLeague(10L, 55L);

    assertEquals(LeagueStatus.ACTIVE, league.getStatus());
    assertEquals(InitialStipendStatus.PENDING, league.getInitialStipendStatus());
    assertNull(league.getInitialStipendIssuedAt());
    verify(transactionServiceClient, never()).issueInitialStipends(any(), any());
  }

  @Test
  void startLeague_duringRegularSeason_issuesInitialStipendsImmediately() {
    League league = inactiveLeague(55L, 10L);

    when(leagueRepository.findById(55L)).thenReturn(Optional.of(league));
    when(leagueMemberRepository.countByLeagueId(55L)).thenReturn(3);
    when(ingestionServiceClient.getCurrentWeekOrPreseasonOptional())
        .thenReturn(
            new CurrentWeekResponse(
                2026, "2", "Regular", 4, "Week 4", Instant.now(), Instant.now()));

    service.startLeague(10L, 55L);

    assertEquals(LeagueStatus.ACTIVE, league.getStatus());
    assertEquals(InitialStipendStatus.ISSUED, league.getInitialStipendStatus());
    verify(transactionServiceClient).issueInitialStipends(55L, bd("100.0000"));
  }

  @Test
  void joinLeague_consumesInviteAndCreatesMembershipWithoutCreatingWallet() {
    League league = inactiveLeague(55L, 10L);
    LeagueInvite invite = new LeagueInvite();
    invite.setId(88L);
    invite.setCode("ABC123");
    invite.setLeague(league);
    invite.setUsesCount(0);
    invite.setMaxUses(5);
    invite.setExpiresAt(OffsetDateTime.now().plusDays(1));

    when(leagueRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(league));
    when(leagueInviteRepository.findByCodeAndRevokedAtIsNull("ABC123")).thenReturn(Optional.of(invite));
    when(leagueMemberRepository.countByLeagueId(55L)).thenReturn(1);
    when(leagueMemberRepository.findByLeagueIdAndUserId(55L, 42L)).thenReturn(Optional.empty());
    when(leagueInviteRepository.incrementUsesCount(88L)).thenReturn(1);

    var response = service.joinLeague(42L, 55L, new JoinLeagueRequest("ABC123"));

    assertEquals(42L, response.userId());
    assertEquals("MEMBER", response.role());
    verify(leagueMemberRepository).save(any(LeagueMember.class));
    verify(transactionServiceClient, never()).createWallet(any());
  }

  private static League inactiveLeague(Long leagueId, Long ownerUserId) {
    League league = new League();
    league.setId(leagueId);
    league.setOwnerUserId(ownerUserId);
    league.setName("Test League");
    league.setStatus(LeagueStatus.INACTIVE);
    league.setMaxMembers(10);
    league.setInitialStipendAmount(bd("100.0000"));
    league.setWeeklyStipendAmount(bd("25.0000"));
    league.setSeasonStartAt(OffsetDateTime.now().plusDays(7));
    league.setSeasonEndAt(OffsetDateTime.now().plusDays(90));
    return league;
  }

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }
}
