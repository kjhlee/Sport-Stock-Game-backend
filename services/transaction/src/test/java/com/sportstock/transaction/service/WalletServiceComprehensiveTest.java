package com.sportstock.transaction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportstock.common.dto.stock_market.StockResponse;
import com.sportstock.common.dto.transaction.StipendResultResponse;
import com.sportstock.common.dto.transaction.StockTransactionRequest;
import com.sportstock.common.dto.transaction.TransactionResponse;
import com.sportstock.common.dto.transaction.WalletResponse;
import com.sportstock.transaction.client.LeagueServiceClient;
import com.sportstock.transaction.client.PortfolioServiceClient;
import com.sportstock.transaction.client.StockMarketServiceClient;
import com.sportstock.transaction.entity.Transaction;
import com.sportstock.transaction.entity.Wallet;
import com.sportstock.transaction.enums.TransactionType;
import com.sportstock.transaction.exception.InvalidTradeRequestException;
import com.sportstock.transaction.exception.TransactionAccessDeniedException;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService Comprehensive Tests")
class WalletServiceComprehensiveTest {

  private static final Long USER_ID = 42L;
  private static final Long LEAGUE_ID = 7L;
  private static final UUID STOCK_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Mock private WalletRepository walletRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private LeagueServiceClient leagueServiceClient;
  @Mock private PortfolioServiceClient portfolioServiceClient;
  @Mock private StockMarketServiceClient stockMarketServiceClient;

  private WalletService service;

  @BeforeEach
  void setUp() {
    service =
        new WalletService(
            walletRepository,
            transactionRepository,
            new NoOpTransactionManager(),
            leagueServiceClient,
            portfolioServiceClient,
            stockMarketServiceClient);
  }

  @Nested
  @DisplayName("Read Path Authorization")
  class ReadPathAuthorization {

    @Test
    void getWallet_requiresExistingWalletMembership() {
      when(walletRepository.existsByUserIdAndLeagueId(USER_ID, LEAGUE_ID)).thenReturn(false);

      assertThrows(
          TransactionAccessDeniedException.class, () -> service.getWallet(USER_ID, LEAGUE_ID));
    }

    @Test
    void getWallet_returnsWalletWithoutCallingLeagueService() {
      Wallet wallet = wallet(USER_ID, LEAGUE_ID, "500.00");

      when(walletRepository.existsByUserIdAndLeagueId(USER_ID, LEAGUE_ID)).thenReturn(true);
      when(walletRepository.findByUserIdAndLeagueId(USER_ID, LEAGUE_ID))
          .thenReturn(Optional.of(wallet));

      WalletResponse response = service.getWallet(USER_ID, LEAGUE_ID);

      assertEquals(bd("500.00"), response.balance());
      verify(leagueServiceClient, never()).getMemberUserIdsInternal(any());
    }

    @Test
    void getLeagueWallets_returnsAllWalletsForLeague() {
      when(walletRepository.existsByUserIdAndLeagueId(USER_ID, LEAGUE_ID)).thenReturn(true);
      when(walletRepository.findAllByLeagueId(LEAGUE_ID))
          .thenReturn(
              List.of(
                  wallet(USER_ID, LEAGUE_ID, "500.00"),
                  wallet(43L, LEAGUE_ID, "250.00"),
                  wallet(44L, LEAGUE_ID, "900.00")));

      List<WalletResponse> responses = service.getLeagueWallets(USER_ID, LEAGUE_ID);

      assertEquals(3, responses.size());
      verify(leagueServiceClient, never()).getMemberUserIdsInternal(any());
    }

    @Test
    void getTransactionHistory_readsFromLocalWalletAuthOnly() {
      when(walletRepository.existsByUserIdAndLeagueId(USER_ID, LEAGUE_ID)).thenReturn(true);
      when(transactionRepository.findByUserIdAndLeagueIdOrderByCreatedAtDesc(
              USER_ID, LEAGUE_ID, PageRequest.of(0, 10)))
          .thenReturn(new PageImpl<>(List.of(stipendTransaction(wallet(USER_ID, LEAGUE_ID, "0.00")))));

      Page<TransactionResponse> response =
          service.getTransactionHistory(USER_ID, LEAGUE_ID, PageRequest.of(0, 10));

      assertEquals(1, response.getContent().size());
      verify(leagueServiceClient, never()).getMemberUserIdsInternal(any());
    }
  }

  @Nested
  @DisplayName("Trade Paths")
  class TradePaths {

    @Test
    void buyStock_rejectsMissingWalletBeforeStockLookup() {
      when(walletRepository.existsByUserIdAndLeagueId(USER_ID, LEAGUE_ID)).thenReturn(false);

      assertThrows(
          TransactionAccessDeniedException.class,
          () ->
              service.processStockBuy(
                  USER_ID,
                  LEAGUE_ID,
                  new StockTransactionRequest(LEAGUE_ID, STOCK_ID, bd("1.0000"), null, null, "buy")));

      verify(stockMarketServiceClient, never()).getStock(any());
      verify(leagueServiceClient, never()).getMemberUserIdsInternal(any());
    }

    @Test
    void buyStock_succeedsWithoutLeagueMembershipHttpCall() {
      Wallet wallet = wallet(USER_ID, LEAGUE_ID, "1000.00");

      when(walletRepository.existsByUserIdAndLeagueId(USER_ID, LEAGUE_ID)).thenReturn(true);
      when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(activeStock("50.00"));
      when(transactionRepository.findByIdempotencyKey("buy-1")).thenReturn(Optional.empty());
      when(walletRepository.findByUserIdAndLeagueIdForUpdate(USER_ID, LEAGUE_ID))
          .thenReturn(Optional.of(wallet));
      stubWalletSave();
      stubTransactionSave();

      TransactionResponse response =
          service.processStockBuy(
              USER_ID,
              LEAGUE_ID,
              new StockTransactionRequest(LEAGUE_ID, STOCK_ID, bd("5.0000"), null, null, "buy-1"));

      assertEquals("STOCK_BUY", response.type());
      assertEquals(0, bd("250.0000").compareTo(response.amount()));
      verify(leagueServiceClient, never()).getMemberUserIdsInternal(any());
    }

    @Test
    void sellStock_rejectsMissingBuyTransactionId() {
      when(walletRepository.existsByUserIdAndLeagueId(USER_ID, LEAGUE_ID)).thenReturn(true);
      when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(activeStock("50.00"));

      assertThrows(
          InvalidTradeRequestException.class,
          () ->
              service.processStockSell(
                  USER_ID,
                  LEAGUE_ID,
                  new StockTransactionRequest(
                      LEAGUE_ID, STOCK_ID, bd("1.0000"), null, null, "sell-missing")));
    }

    @Test
    void sellStock_allowsPartialSalesAgainstSameBuyTransaction() {
      Wallet wallet = wallet(USER_ID, LEAGUE_ID, "500.00");
      Transaction buyTransaction = buyTransaction(wallet, "50.00", "500.00");

      when(walletRepository.existsByUserIdAndLeagueId(USER_ID, LEAGUE_ID)).thenReturn(true);
      when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(activeStock("50.00"));
      when(transactionRepository.findById(900L)).thenReturn(Optional.of(buyTransaction));
      when(transactionRepository.findByIdempotencyKey("sell-1")).thenReturn(Optional.empty());
      when(transactionRepository.sumSoldQuantityByBuyTransactionId(900L)).thenReturn(bd("3.0000"));
      when(walletRepository.findByUserIdAndLeagueIdForUpdate(USER_ID, LEAGUE_ID))
          .thenReturn(Optional.of(wallet));
      stubWalletSave();
      stubTransactionSave();

      TransactionResponse response =
          service.processStockSell(
              USER_ID,
              LEAGUE_ID,
              new StockTransactionRequest(LEAGUE_ID, STOCK_ID, bd("5.0000"), null, 900L, "sell-1"));

      assertEquals("STOCK_SELL", response.type());
      assertEquals(0, bd("225.0000").compareTo(response.amount()));
      verify(leagueServiceClient, never()).getMemberUserIdsInternal(any());
    }
  }

  @Nested
  @DisplayName("Stipend Paths")
  class StipendPaths {

    @Test
    void issueInitialStipends_createsWalletsWhenMissing() {
      List<Long> userIds = Arrays.asList(USER_ID, 43L, 44L);

      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID)).thenReturn(userIds);
      for (Long userId : userIds) {
        when(walletRepository.existsByUserIdAndLeagueId(userId, LEAGUE_ID)).thenReturn(false);
        when(walletRepository.findByUserIdAndLeagueIdForUpdate(userId, LEAGUE_ID))
            .thenReturn(Optional.of(wallet(userId, LEAGUE_ID, "0.00")));
        when(transactionRepository.existsByIdempotencyKey(
                "INITIAL_STIPEND:" + LEAGUE_ID + ":" + userId))
            .thenReturn(false);
      }
      stubWalletSave();
      stubTransactionSave();

      StipendResultResponse response = service.issueInitialStipends(LEAGUE_ID, bd("1000.00"), null);

      assertEquals(3, response.walletsCreated());
      assertEquals(3, response.stipendsIssued());
    }

    @Test
    void issueWeeklyStipends_isIdempotentPerWeek() {
      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID)).thenReturn(List.of(USER_ID));
      when(transactionRepository.existsByIdempotencyKey("WEEKLY_STIPEND:7:42:3")).thenReturn(true);

      StipendResultResponse response = service.issueWeeklyStipends(LEAGUE_ID, bd("100.00"), 3);

      assertEquals(0, response.stipendsIssued());
      verify(walletRepository, never()).findByUserIdAndLeagueIdForUpdate(any(), any());
    }
  }

  private void stubWalletSave() {
    when(walletRepository.save(any(Wallet.class)))
        .thenAnswer(
            invocation -> {
              Wallet wallet = invocation.getArgument(0);
              if (wallet.getId() == null) {
                wallet.setId(100L);
              }
              if (wallet.getCreatedAt() == null) {
                wallet.setCreatedAt(OffsetDateTime.now());
              }
              wallet.setUpdatedAt(OffsetDateTime.now());
              return wallet;
            });
  }

  private void stubTransactionSave() {
    when(transactionRepository.save(any(Transaction.class)))
        .thenAnswer(
            invocation -> {
              Transaction transaction = invocation.getArgument(0);
              if (transaction.getId() == null) {
                transaction.setId(200L);
              }
              if (transaction.getCreatedAt() == null) {
                transaction.setCreatedAt(OffsetDateTime.now());
              }
              return transaction;
            });
  }

  private Wallet wallet(Long userId, Long leagueId, String balance) {
    Wallet wallet = new Wallet();
    wallet.setId(100L);
    wallet.setUserId(userId);
    wallet.setLeagueId(leagueId);
    wallet.setBalance(bd(balance));
    wallet.setCreatedAt(OffsetDateTime.now());
    wallet.setUpdatedAt(OffsetDateTime.now());
    return wallet;
  }

  private Transaction buyTransaction(Wallet wallet, String pricePerShare, String amount) {
    Transaction transaction = new Transaction();
    transaction.setId(900L);
    transaction.setWallet(wallet);
    transaction.setType(TransactionType.STOCK_BUY);
    transaction.setLeagueId(wallet.getLeagueId());
    transaction.setUserId(wallet.getUserId());
    transaction.setReferenceId("stock:" + STOCK_ID);
    transaction.setPricePerShare(bd(pricePerShare));
    transaction.setAmount(bd(amount));
    transaction.setCreatedAt(OffsetDateTime.now());
    return transaction;
  }

  private Transaction stipendTransaction(Wallet wallet) {
    Transaction transaction = new Transaction();
    transaction.setId(800L);
    transaction.setWallet(wallet);
    transaction.setType(TransactionType.INITIAL_STIPEND);
    transaction.setLeagueId(wallet.getLeagueId());
    transaction.setUserId(wallet.getUserId());
    transaction.setAmount(bd("1000.00"));
    transaction.setBalanceBefore(bd("0.00"));
    transaction.setBalanceAfter(bd("1000.00"));
    transaction.setCreatedAt(OffsetDateTime.now());
    return transaction;
  }

  private StockResponse activeStock(String currentPrice) {
    return new StockResponse(
        STOCK_ID,
        "3139477",
        "Patrick Mahomes",
        "QB",
        "PLAYER",
        "12",
        bd(currentPrice),
        "ACTIVE",
        false,
        false,
        Instant.now());
  }

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  private static final class NoOpTransactionManager extends AbstractPlatformTransactionManager {
    @Override
    protected Object doGetTransaction() {
      return new Object();
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {}

    @Override
    protected void doCommit(DefaultTransactionStatus status) {}

    @Override
    protected void doRollback(DefaultTransactionStatus status) {}
  }
}
