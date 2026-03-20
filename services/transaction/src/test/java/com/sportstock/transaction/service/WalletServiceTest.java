package com.sportstock.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportstock.common.dto.transaction.StipendResultResponse;
import com.sportstock.common.dto.transaction.StockTransactionRequest;
import com.sportstock.common.dto.transaction.TransactionResponse;
import com.sportstock.common.dto.transaction.WalletResponse;
import com.sportstock.common.dto.stock_market.StockResponse;
import com.sportstock.transaction.client.LeagueServiceClient;
import com.sportstock.transaction.client.StockMarketServiceClient;
import com.sportstock.transaction.entity.Transaction;
import com.sportstock.transaction.entity.Wallet;
import com.sportstock.transaction.enums.TransactionType;
import com.sportstock.transaction.exception.WalletAlreadyExistsException;
import com.sportstock.transaction.exception.WalletNotFoundException;
import com.sportstock.transaction.repo.TransactionRepository;
import com.sportstock.transaction.repo.WalletRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService Unit Tests")
class WalletServiceTest {

  @Mock private WalletRepository walletRepository;

  @Mock private TransactionRepository transactionRepository;

  @Mock private PlatformTransactionManager txManager;

  @Mock private LeagueServiceClient leagueServiceClient;
  @Mock private StockMarketServiceClient stockMarketServiceClient;

  private WalletService walletService;

  @Captor private ArgumentCaptor<Wallet> walletCaptor;

  private static final Long TEST_USER_ID = 1001L;
  private static final Long TEST_LEAGUE_ID = 1L;
  private static final BigDecimal INITIAL_BALANCE = BigDecimal.ZERO;

  @BeforeEach
  void setUp() {
    walletService =
        new WalletService(
            walletRepository,
            transactionRepository,
            txManager,
            leagueServiceClient,
            stockMarketServiceClient);
  }

  @Nested
  @DisplayName("createWallet() tests")
  class CreateWalletTests {

    @Test
    @DisplayName("Should successfully create a new wallet")
    void shouldCreateNewWallet() {
      // Given
      when(walletRepository.existsByUserIdAndLeagueId(TEST_USER_ID, TEST_LEAGUE_ID))
          .thenReturn(false);

      Wallet savedWallet = createMockWallet(1L, TEST_USER_ID, TEST_LEAGUE_ID, INITIAL_BALANCE);
      when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

      // When
      WalletResponse response = walletService.createWallet(TEST_USER_ID, TEST_LEAGUE_ID);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.userId()).isEqualTo(TEST_USER_ID);
      assertThat(response.leagueId()).isEqualTo(TEST_LEAGUE_ID);
      assertThat(response.balance()).isEqualByComparingTo(INITIAL_BALANCE);

      verify(walletRepository).existsByUserIdAndLeagueId(TEST_USER_ID, TEST_LEAGUE_ID);
      verify(walletRepository).save(walletCaptor.capture());

      Wallet capturedWallet = walletCaptor.getValue();
      assertThat(capturedWallet.getUserId()).isEqualTo(TEST_USER_ID);
      assertThat(capturedWallet.getLeagueId()).isEqualTo(TEST_LEAGUE_ID);
      assertThat(capturedWallet.getBalance()).isEqualByComparingTo(INITIAL_BALANCE);
    }

    @Test
    @DisplayName("Should throw WalletAlreadyExistsException when wallet exists")
    void shouldThrowExceptionWhenWalletExists() {
      // Given
      when(walletRepository.existsByUserIdAndLeagueId(TEST_USER_ID, TEST_LEAGUE_ID))
          .thenReturn(true);

      // When & Then
      assertThatThrownBy(() -> walletService.createWallet(TEST_USER_ID, TEST_LEAGUE_ID))
          .isInstanceOf(WalletAlreadyExistsException.class)
          .hasMessageContaining("userId=" + TEST_USER_ID)
          .hasMessageContaining("leagueId=" + TEST_LEAGUE_ID);

      verify(walletRepository).existsByUserIdAndLeagueId(TEST_USER_ID, TEST_LEAGUE_ID);
      verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should throw WalletAlreadyExistsException on DataIntegrityViolationException")
    void shouldHandleConcurrentCreation() {
      // Given
      when(walletRepository.existsByUserIdAndLeagueId(TEST_USER_ID, TEST_LEAGUE_ID))
          .thenReturn(false);
      when(walletRepository.save(any(Wallet.class)))
          .thenThrow(new DataIntegrityViolationException("Duplicate key"));

      // When & Then
      assertThatThrownBy(() -> walletService.createWallet(TEST_USER_ID, TEST_LEAGUE_ID))
          .isInstanceOf(WalletAlreadyExistsException.class);

      verify(walletRepository).save(any(Wallet.class));
    }
  }

  @Nested
  @DisplayName("getWallet() tests")
  class GetWalletTests {

    @Test
    @DisplayName("Should successfully retrieve wallet")
    void shouldGetWallet() {
      // Given
      Wallet wallet = createMockWallet(1L, TEST_USER_ID, TEST_LEAGUE_ID, new BigDecimal("1000.00"));
      when(walletRepository.findByUserIdAndLeagueId(TEST_USER_ID, TEST_LEAGUE_ID))
          .thenReturn(Optional.of(wallet));

      // When
      WalletResponse response = walletService.getWallet(TEST_USER_ID, TEST_LEAGUE_ID);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.id()).isEqualTo(1L);
      assertThat(response.userId()).isEqualTo(TEST_USER_ID);
      assertThat(response.leagueId()).isEqualTo(TEST_LEAGUE_ID);
      assertThat(response.balance()).isEqualByComparingTo(new BigDecimal("1000.00"));

      verify(walletRepository).findByUserIdAndLeagueId(TEST_USER_ID, TEST_LEAGUE_ID);
    }

    @Test
    @DisplayName("Should throw WalletNotFoundException when wallet not found")
    void shouldThrowExceptionWhenWalletNotFound() {
      // Given
      when(walletRepository.findByUserIdAndLeagueId(TEST_USER_ID, TEST_LEAGUE_ID))
          .thenReturn(Optional.empty());

      // When & Then
      assertThatThrownBy(() -> walletService.getWallet(TEST_USER_ID, TEST_LEAGUE_ID))
          .isInstanceOf(WalletNotFoundException.class)
          .hasMessageContaining("user: " + TEST_USER_ID);

      verify(walletRepository).findByUserIdAndLeagueId(TEST_USER_ID, TEST_LEAGUE_ID);
    }
  }

  @Nested
  @DisplayName("getLeagueWallets() tests")
  class GetLeagueWalletsTests {

    @Test
    @DisplayName("Should retrieve all wallets for a league")
    void shouldGetAllLeagueWallets() {
      // Given
      List<Wallet> wallets =
          Arrays.asList(
              createMockWallet(1L, 1001L, TEST_LEAGUE_ID, new BigDecimal("5000.00")),
              createMockWallet(2L, 1002L, TEST_LEAGUE_ID, new BigDecimal("6000.00")),
              createMockWallet(3L, 1003L, TEST_LEAGUE_ID, new BigDecimal("7000.00")));
      when(walletRepository.findAllByLeagueId(TEST_LEAGUE_ID)).thenReturn(wallets);

      // When
      List<WalletResponse> responses = walletService.getLeagueWallets(TEST_LEAGUE_ID);

      // Then
      assertThat(responses).hasSize(3);
      assertThat(responses.get(0).userId()).isEqualTo(1001L);
      assertThat(responses.get(1).userId()).isEqualTo(1002L);
      assertThat(responses.get(2).userId()).isEqualTo(1003L);
      assertThat(responses.get(0).balance()).isEqualByComparingTo(new BigDecimal("5000.00"));

      verify(walletRepository).findAllByLeagueId(TEST_LEAGUE_ID);
    }

    @Test
    @DisplayName("Should return empty list when no wallets exist")
    void shouldReturnEmptyListWhenNoWallets() {
      // Given
      when(walletRepository.findAllByLeagueId(TEST_LEAGUE_ID)).thenReturn(List.of());

      // When
      List<WalletResponse> responses = walletService.getLeagueWallets(TEST_LEAGUE_ID);

      // Then
      assertThat(responses).isEmpty();
      verify(walletRepository).findAllByLeagueId(TEST_LEAGUE_ID);
    }
  }

  @Nested
  @DisplayName("getTransactionHistory() tests")
  class GetTransactionHistoryTests {

    @Test
    @DisplayName("Should retrieve paginated transaction history")
    void shouldGetTransactionHistory() {
      // Given
      Wallet wallet =
          createMockWallet(1L, TEST_USER_ID, TEST_LEAGUE_ID, new BigDecimal("10000.00"));

      Transaction tx1 =
          createMockTransaction(
              1L,
              wallet,
              TransactionType.INITIAL_STIPEND,
              new BigDecimal("10000.00"),
              BigDecimal.ZERO,
              new BigDecimal("10000.00"));
      Transaction tx2 =
          createMockTransaction(
              2L,
              wallet,
              TransactionType.WEEKLY_STIPEND,
              new BigDecimal("500.00"),
              new BigDecimal("10000.00"),
              new BigDecimal("10500.00"));

      Page<Transaction> transactionPage =
          new PageImpl<>(Arrays.asList(tx2, tx1), PageRequest.of(0, 20), 2);

      when(transactionRepository.findByUserIdAndLeagueIdOrderByCreatedAtDesc(
              eq(TEST_USER_ID), eq(TEST_LEAGUE_ID), any(Pageable.class)))
          .thenReturn(transactionPage);

      // When
      Page<TransactionResponse> responses =
          walletService.getTransactionHistory(TEST_USER_ID, TEST_LEAGUE_ID, PageRequest.of(0, 20));

      // Then
      assertThat(responses.getContent()).hasSize(2);
      assertThat(responses.getTotalElements()).isEqualTo(2);
      assertThat(responses.getContent().get(0).type()).isEqualTo("WEEKLY_STIPEND");
      assertThat(responses.getContent().get(1).type()).isEqualTo("INITIAL_STIPEND");

      verify(transactionRepository)
          .findByUserIdAndLeagueIdOrderByCreatedAtDesc(
              eq(TEST_USER_ID), eq(TEST_LEAGUE_ID), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return empty page when no transactions exist")
    void shouldReturnEmptyPageWhenNoTransactions() {
      // Given
      Page<Transaction> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
      when(transactionRepository.findByUserIdAndLeagueIdOrderByCreatedAtDesc(
              eq(TEST_USER_ID), eq(TEST_LEAGUE_ID), any(Pageable.class)))
          .thenReturn(emptyPage);

      // When
      Page<TransactionResponse> responses =
          walletService.getTransactionHistory(TEST_USER_ID, TEST_LEAGUE_ID, PageRequest.of(0, 20));

      // Then
      assertThat(responses.getContent()).isEmpty();
      assertThat(responses.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Should handle pagination correctly")
    void shouldHandlePaginationCorrectly() {
      // Given
      Wallet wallet =
          createMockWallet(1L, TEST_USER_ID, TEST_LEAGUE_ID, new BigDecimal("10000.00"));

      List<Transaction> allTransactions =
          Arrays.asList(
              createMockTransaction(
                  1L,
                  wallet,
                  TransactionType.INITIAL_STIPEND,
                  new BigDecimal("10000.00"),
                  BigDecimal.ZERO,
                  new BigDecimal("10000.00")),
              createMockTransaction(
                  2L,
                  wallet,
                  TransactionType.WEEKLY_STIPEND,
                  new BigDecimal("500.00"),
                  new BigDecimal("10000.00"),
                  new BigDecimal("10500.00")),
              createMockTransaction(
                  3L,
                  wallet,
                  TransactionType.WEEKLY_STIPEND,
                  new BigDecimal("500.00"),
                  new BigDecimal("10500.00"),
                  new BigDecimal("11000.00")));

      Page<Transaction> page1 =
          new PageImpl<>(allTransactions.subList(0, 2), PageRequest.of(0, 2), 3);

      Page<Transaction> page2 =
          new PageImpl<>(allTransactions.subList(2, 3), PageRequest.of(1, 2), 3);

      when(transactionRepository.findByUserIdAndLeagueIdOrderByCreatedAtDesc(
              eq(TEST_USER_ID), eq(TEST_LEAGUE_ID), eq(PageRequest.of(0, 2))))
          .thenReturn(page1);

      when(transactionRepository.findByUserIdAndLeagueIdOrderByCreatedAtDesc(
              eq(TEST_USER_ID), eq(TEST_LEAGUE_ID), eq(PageRequest.of(1, 2))))
          .thenReturn(page2);

      // When
      Page<TransactionResponse> firstPage =
          walletService.getTransactionHistory(TEST_USER_ID, TEST_LEAGUE_ID, PageRequest.of(0, 2));
      Page<TransactionResponse> secondPage =
          walletService.getTransactionHistory(TEST_USER_ID, TEST_LEAGUE_ID, PageRequest.of(1, 2));

      // Then
      assertThat(firstPage.getContent()).hasSize(2);
      assertThat(firstPage.getTotalElements()).isEqualTo(3);
      assertThat(firstPage.getTotalPages()).isEqualTo(2);

      assertThat(secondPage.getContent()).hasSize(1);
      assertThat(secondPage.getTotalElements()).isEqualTo(3);
      assertThat(secondPage.isLast()).isTrue();
    }
  }

  @Nested
  @DisplayName("issueInitialStipends() tests")
  class IssueInitialStipendsTests {

    @Test
    @DisplayName("Should return stipend result with league info")
    void shouldReturnStipendResultWithLeagueInfo() {
      // Given
      BigDecimal amount = new BigDecimal("10000.00");

      when(leagueServiceClient.getMemberUserIds(TEST_LEAGUE_ID)).thenReturn(List.of(TEST_USER_ID));
      Wallet wallet = createMockWallet(1L, TEST_USER_ID, TEST_LEAGUE_ID, INITIAL_BALANCE);
      when(walletRepository.findByUserIdAndLeagueIdForUpdate(TEST_USER_ID, TEST_LEAGUE_ID))
          .thenReturn(Optional.of(wallet));
      when(transactionRepository.existsByIdempotencyKey(any())).thenReturn(false);

      // When
      StipendResultResponse response = walletService.issueInitialStipends(TEST_LEAGUE_ID, amount);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.leagueId()).isEqualTo(TEST_LEAGUE_ID);
      assertThat(response.amountPerUser()).isEqualByComparingTo(amount);
    }

    @Test
    @DisplayName("Should handle empty user list")
    void shouldHandleEmptyUserList() {
      // Given
      BigDecimal amount = new BigDecimal("10000.00");
      when(leagueServiceClient.getMemberUserIds(TEST_LEAGUE_ID)).thenReturn(List.of());

      // When
      StipendResultResponse response = walletService.issueInitialStipends(TEST_LEAGUE_ID, amount);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.leagueId()).isEqualTo(TEST_LEAGUE_ID);
      assertThat(response.walletsCreated()).isZero();
      assertThat(response.stipendsIssued()).isZero();
    }
  }

  @Nested
  @DisplayName("issueWeeklyStipends() tests")
  class IssueWeeklyStipendsTests {

    @Test
    @DisplayName("Should return stipend result with league info")
    void shouldReturnStipendResultWithLeagueInfo() {
      // Given
      BigDecimal amount = new BigDecimal("500.00");
      Integer weekNumber = 1;

      when(leagueServiceClient.getMemberUserIds(TEST_LEAGUE_ID)).thenReturn(List.of(TEST_USER_ID));
      Wallet wallet =
          createMockWallet(1L, TEST_USER_ID, TEST_LEAGUE_ID, new BigDecimal("10000.00"));
      when(walletRepository.findByUserIdAndLeagueIdForUpdate(TEST_USER_ID, TEST_LEAGUE_ID))
          .thenReturn(Optional.of(wallet));
      when(transactionRepository.existsByIdempotencyKey(any())).thenReturn(false);

      // When
      StipendResultResponse response =
          walletService.issueWeeklyStipends(TEST_LEAGUE_ID, amount, weekNumber);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.leagueId()).isEqualTo(TEST_LEAGUE_ID);
      assertThat(response.amountPerUser()).isEqualByComparingTo(amount);
      assertThat(response.walletsCreated()).isZero(); // Weekly stipends don't create wallets
    }

    @Test
    @DisplayName("Should handle empty user list")
    void shouldHandleEmptyUserList() {
      // Given
      BigDecimal amount = new BigDecimal("500.00");
      Integer weekNumber = 1;
      when(leagueServiceClient.getMemberUserIds(TEST_LEAGUE_ID)).thenReturn(List.of());

      // When
      StipendResultResponse response =
          walletService.issueWeeklyStipends(TEST_LEAGUE_ID, amount, weekNumber);

      // Then
      assertThat(response).isNotNull();
      assertThat(response.leagueId()).isEqualTo(TEST_LEAGUE_ID);
      assertThat(response.stipendsIssued()).isZero();
    }

    @Test
    @DisplayName("Should handle multiple week numbers")
    void shouldHandleMultipleWeekNumbers() {
      // Given
      BigDecimal amount = new BigDecimal("500.00");

      when(leagueServiceClient.getMemberUserIds(TEST_LEAGUE_ID)).thenReturn(List.of(TEST_USER_ID));
      Wallet wallet =
          createMockWallet(1L, TEST_USER_ID, TEST_LEAGUE_ID, new BigDecimal("10000.00"));
      when(walletRepository.findByUserIdAndLeagueIdForUpdate(TEST_USER_ID, TEST_LEAGUE_ID))
          .thenReturn(Optional.of(wallet));
      when(transactionRepository.existsByIdempotencyKey(any())).thenReturn(false);

      // When
      StipendResultResponse response1 =
          walletService.issueWeeklyStipends(TEST_LEAGUE_ID, amount, 1);
      StipendResultResponse response2 =
          walletService.issueWeeklyStipends(TEST_LEAGUE_ID, amount, 2);
      StipendResultResponse response3 =
          walletService.issueWeeklyStipends(TEST_LEAGUE_ID, amount, 18);

      // Then
      assertThat(response1).isNotNull();
      assertThat(response2).isNotNull();
      assertThat(response3).isNotNull();
    }
  }

  @Nested
  @DisplayName("processStockBuy() tests")
  class ProcessStockBuyTests {

    @Test
    @DisplayName("Should throw UnsupportedOperationException (not yet implemented)")
    void shouldThrowUnsupportedOperationForStockBuy() {
      when(stockMarketServiceClient.getStock(any())).thenReturn(createActiveStockResponse());

      // When & Then
      assertThatThrownBy(
              () ->
                  walletService.processStockBuy(
                      TEST_USER_ID,
                      TEST_LEAGUE_ID,
                      new StockTransactionRequest(
                          TEST_LEAGUE_ID,
                          UUID.randomUUID(),
                          new BigDecimal("10"),
                          null,
                          "idem-buy-1")))
          .isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("TODO");
    }

    @Test
    @DisplayName("Should validate all parameters are passed")
    void shouldValidateAllParametersArePassed() {
      when(stockMarketServiceClient.getStock(any())).thenReturn(createActiveStockResponse());

      // When & Then
      assertThatThrownBy(
              () ->
                  walletService.processStockBuy(
                      1001L,
                      1L,
                      new StockTransactionRequest(
                          1L, UUID.randomUUID(), null, new BigDecimal("250.50"), "idem-buy-2")))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Nested
  @DisplayName("processStockSell() tests")
  class ProcessStockSellTests {

    @Test
    @DisplayName("Should throw UnsupportedOperationException (not yet implemented)")
    void shouldThrowUnsupportedOperationForStockSell() {
      when(stockMarketServiceClient.getStock(any())).thenReturn(createActiveStockResponse());

      // When & Then
      assertThatThrownBy(
              () ->
                  walletService.processStockSell(
                      TEST_USER_ID,
                      TEST_LEAGUE_ID,
                      new StockTransactionRequest(
                          TEST_LEAGUE_ID,
                          UUID.randomUUID(),
                          new BigDecimal("10"),
                          null,
                          "idem-sell-1")))
          .isInstanceOf(UnsupportedOperationException.class)
          .hasMessageContaining("TODO");
    }

    @Test
    @DisplayName("Should validate all parameters are passed")
    void shouldValidateAllParametersArePassed() {
      when(stockMarketServiceClient.getStock(any())).thenReturn(createActiveStockResponse());

      // When & Then
      assertThatThrownBy(
              () ->
                  walletService.processStockSell(
                      1002L,
                      2L,
                      new StockTransactionRequest(
                          2L, UUID.randomUUID(), null, new BigDecimal("175.75"), "idem-sell-2")))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Nested
  @DisplayName("Edge Cases and Boundary Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle very large balance amounts")
    void shouldHandleLargeBalances() {
      // Given
      BigDecimal largeAmount = new BigDecimal("999999999.9999");
      Wallet wallet = createMockWallet(1L, TEST_USER_ID, TEST_LEAGUE_ID, largeAmount);
      when(walletRepository.findByUserIdAndLeagueId(TEST_USER_ID, TEST_LEAGUE_ID))
          .thenReturn(Optional.of(wallet));

      // When
      WalletResponse response = walletService.getWallet(TEST_USER_ID, TEST_LEAGUE_ID);

      // Then
      assertThat(response.balance()).isEqualByComparingTo(largeAmount);
    }

    @Test
    @DisplayName("Should handle zero balance")
    void shouldHandleZeroBalance() {
      // Given
      Wallet wallet = createMockWallet(1L, TEST_USER_ID, TEST_LEAGUE_ID, BigDecimal.ZERO);
      when(walletRepository.findByUserIdAndLeagueId(TEST_USER_ID, TEST_LEAGUE_ID))
          .thenReturn(Optional.of(wallet));

      // When
      WalletResponse response = walletService.getWallet(TEST_USER_ID, TEST_LEAGUE_ID);

      // Then
      assertThat(response.balance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should handle multiple leagues for same user")
    void shouldHandleMultipleLeaguesForSameUser() {
      // Given
      Long league1 = 1L;
      Long league2 = 2L;

      Wallet wallet1 = createMockWallet(1L, TEST_USER_ID, league1, new BigDecimal("5000.00"));
      Wallet wallet2 = createMockWallet(2L, TEST_USER_ID, league2, new BigDecimal("7000.00"));

      when(walletRepository.findByUserIdAndLeagueId(TEST_USER_ID, league1))
          .thenReturn(Optional.of(wallet1));
      when(walletRepository.findByUserIdAndLeagueId(TEST_USER_ID, league2))
          .thenReturn(Optional.of(wallet2));

      // When
      WalletResponse response1 = walletService.getWallet(TEST_USER_ID, league1);
      WalletResponse response2 = walletService.getWallet(TEST_USER_ID, league2);

      // Then
      assertThat(response1.leagueId()).isEqualTo(league1);
      assertThat(response2.leagueId()).isEqualTo(league2);
      assertThat(response1.balance()).isEqualByComparingTo(new BigDecimal("5000.00"));
      assertThat(response2.balance()).isEqualByComparingTo(new BigDecimal("7000.00"));
    }
  }

  // Helper methods

  private Wallet createMockWallet(Long id, Long userId, Long leagueId, BigDecimal balance) {
    Wallet wallet = new Wallet();
    wallet.setId(id);
    wallet.setUserId(userId);
    wallet.setLeagueId(leagueId);
    wallet.setBalance(balance);
    wallet.setCreatedAt(OffsetDateTime.now());
    wallet.setUpdatedAt(OffsetDateTime.now());
    return wallet;
  }

  private Transaction createMockTransaction(
      Long id,
      Wallet wallet,
      TransactionType type,
      BigDecimal amount,
      BigDecimal balanceBefore,
      BigDecimal balanceAfter) {
    Transaction transaction = new Transaction();
    transaction.setId(id);
    transaction.setWallet(wallet);
    transaction.setType(type);
    transaction.setAmount(amount);
    transaction.setBalanceBefore(balanceBefore);
    transaction.setBalanceAfter(balanceAfter);
    transaction.setLeagueId(wallet.getLeagueId());
    transaction.setUserId(wallet.getUserId());
    transaction.setCreatedAt(OffsetDateTime.now());
    return transaction;
  }

  private StockResponse createActiveStockResponse() {
    return new StockResponse(
        UUID.randomUUID(),
        "espn-athlete-1",
        "Test Player",
        "QB",
        "team-1",
        new BigDecimal("25.00"),
        "ACTIVE",
        Instant.now());
  }
}
