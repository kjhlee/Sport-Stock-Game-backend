package com.sportstock.transaction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import com.sportstock.common.dto.stock_market.StockResponse;
import com.sportstock.common.dto.transaction.StockTransactionRequest;
import com.sportstock.transaction.client.IngestionServiceClient;
import com.sportstock.transaction.client.LeagueServiceClient;
import com.sportstock.transaction.client.PortfolioServiceClient;
import com.sportstock.transaction.client.StockMarketServiceClient;
import com.sportstock.transaction.entity.Transaction;
import com.sportstock.transaction.entity.Wallet;
import com.sportstock.transaction.enums.TransactionType;
import com.sportstock.transaction.exception.InsufficientFundsException;
import com.sportstock.transaction.exception.TransactionAccessDeniedException;
import com.sportstock.transaction.repo.TransactionRepository;
import com.sportstock.transaction.repo.WalletRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

  private static final Long USER_ID = 42L;
  private static final Long LEAGUE_ID = 7L;
  private static final UUID STOCK_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Mock private WalletRepository walletRepository;
  @Mock private TransactionRepository transactionRepository;
  @Mock private LeagueServiceClient leagueServiceClient;
  @Mock private IngestionServiceClient ingestionServiceClient;
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
            ingestionServiceClient,
            portfolioServiceClient,
            stockMarketServiceClient);
    when(ingestionServiceClient.getCurrentWeek())
        .thenReturn(new CurrentWeekResponse(2026, "2", "REGULAR", 1, "Week 1", null, null));
  }

  @Test
  void processStockBuy_debitsWalletAndRecordsTransaction() {
    Wallet wallet = wallet(USER_ID, LEAGUE_ID, "100.0000");
    StockResponse stock = activeStock("12.5000");

    when(walletRepository.existsByUserIdAndLeagueId(USER_ID, LEAGUE_ID)).thenReturn(true);
    stubWalletSave();
    stubTransactionSave();
    when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(stock);
    when(
            transactionRepository
                .findByIdempotencyKeyAndLeagueIdAndUserIdAndSeasonYearAndSeasonType(
                    "buy-1", LEAGUE_ID, USER_ID, 2026, "2"))
        .thenReturn(Optional.empty());
    when(walletRepository.findByUserIdAndLeagueIdForUpdate(USER_ID, LEAGUE_ID))
        .thenReturn(Optional.of(wallet));

    var response =
        service.processStockBuy(
            USER_ID,
            LEAGUE_ID,
            new StockTransactionRequest(LEAGUE_ID, STOCK_ID, bd("2.0000"), null, null, "buy-1"));

    assertEquals("STOCK_BUY", response.type());
    assertEquals(bd("25.0000"), response.amount());
    assertEquals(bd("100.0000"), response.balanceBefore());
    assertEquals(bd("75.0000"), response.balanceAfter());
    assertEquals(bd("12.5000"), response.pricePerShare());
    assertEquals(bd("75.0000"), wallet.getBalance());

    ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
    verify(transactionRepository).save(transactionCaptor.capture());
    Transaction saved = transactionCaptor.getValue();
    assertEquals(TransactionType.STOCK_BUY, saved.getType());
    assertEquals("stock:" + STOCK_ID, saved.getReferenceId());
    assertEquals("buy-1", saved.getIdempotencyKey());
    assertEquals(bd("25.0000"), saved.getAmount());
    assertEquals(bd("12.5000"), saved.getPricePerShare());
    verify(leagueServiceClient, never()).getMemberUserIdsInternal(any());
  }

  @Test
  void processStockBuy_rejectsWhenFundsAreInsufficient() {
    Wallet wallet = wallet(USER_ID, LEAGUE_ID, "10.0000");

    when(walletRepository.existsByUserIdAndLeagueId(USER_ID, LEAGUE_ID)).thenReturn(true);
    when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(activeStock("12.5000"));
    when(
            transactionRepository
                .findByIdempotencyKeyAndLeagueIdAndUserIdAndSeasonYearAndSeasonType(
                    "buy-2", LEAGUE_ID, USER_ID, 2026, "2"))
        .thenReturn(Optional.empty());
    when(walletRepository.findByUserIdAndLeagueIdForUpdate(USER_ID, LEAGUE_ID))
        .thenReturn(Optional.of(wallet));

    assertThrows(
        InsufficientFundsException.class,
        () ->
            service.processStockBuy(
                USER_ID,
                LEAGUE_ID,
                new StockTransactionRequest(
                    LEAGUE_ID, STOCK_ID, bd("1.0000"), null, null, "buy-2")));

    assertEquals(bd("10.0000"), wallet.getBalance());
    verify(transactionRepository, never()).save(any(Transaction.class));
    verify(leagueServiceClient, never()).getMemberUserIdsInternal(any());
  }

  @Test
  void processStockBuy_throwsAccessDeniedWhenWalletNotFound() {
    when(walletRepository.existsByUserIdAndLeagueId(USER_ID, LEAGUE_ID)).thenReturn(false);

    assertThrows(
        TransactionAccessDeniedException.class,
        () ->
            service.processStockBuy(
                USER_ID,
                LEAGUE_ID,
                new StockTransactionRequest(
                    LEAGUE_ID, STOCK_ID, bd("1.0000"), null, null, "buy-no-wallet")));

    verify(leagueServiceClient, never()).getMemberUserIdsInternal(any());
    verify(stockMarketServiceClient, never()).getStock(any());
  }

  @Test
  void processStockSell_creditsWalletAtNinetyPercentOfOriginalBuyPrice() {
    Wallet wallet = wallet(USER_ID, LEAGUE_ID, "50.0000");
    Transaction buyTransaction = buyTransaction(wallet, "10.0000", "20.0000");

    when(walletRepository.existsByUserIdAndLeagueId(USER_ID, LEAGUE_ID)).thenReturn(true);
    stubWalletSave();
    stubTransactionSave();
    when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(activeStock("99.0000"));
    when(transactionRepository.findById(900L)).thenReturn(Optional.of(buyTransaction));
    when(transactionRepository.sumSoldQuantityByBuyTransactionId(900L)).thenReturn(BigDecimal.ZERO);
    when(
            transactionRepository
                .findByIdempotencyKeyAndLeagueIdAndUserIdAndSeasonYearAndSeasonType(
                    "sell-1", LEAGUE_ID, USER_ID, 2026, "2"))
        .thenReturn(Optional.empty());
    when(walletRepository.findByUserIdAndLeagueIdForUpdate(USER_ID, LEAGUE_ID))
        .thenReturn(Optional.of(wallet));

    var response =
        service.processStockSell(
            USER_ID,
            LEAGUE_ID,
            new StockTransactionRequest(LEAGUE_ID, STOCK_ID, bd("1.0000"), null, 900L, "sell-1"));

    assertEquals("STOCK_SELL", response.type());
    assertEquals(bd("9.0000"), response.amount());
    assertEquals(bd("9.0000"), response.pricePerShare());
    assertEquals(bd("59.0000"), response.balanceAfter());
    assertEquals(bd("59.0000"), wallet.getBalance());

    ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
    verify(transactionRepository).save(transactionCaptor.capture());
    Transaction saved = transactionCaptor.getValue();
    assertEquals(TransactionType.STOCK_SELL, saved.getType());
    assertEquals(900L, saved.getBuyTransactionId());
    assertEquals(bd("9.0000"), saved.getPricePerShare());
    verify(leagueServiceClient, never()).getMemberUserIdsInternal(any());
  }

  @Test
  void processStockSell_handlesNullSoldQuantityFromRepo() {
    Wallet wallet = wallet(USER_ID, LEAGUE_ID, "50.0000");
    Transaction buyTransaction = buyTransaction(wallet, "10.0000", "20.0000");

    when(walletRepository.existsByUserIdAndLeagueId(USER_ID, LEAGUE_ID)).thenReturn(true);
    stubWalletSave();
    stubTransactionSave();
    when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(activeStock("99.0000"));
    when(transactionRepository.findById(900L)).thenReturn(Optional.of(buyTransaction));
    when(transactionRepository.sumSoldQuantityByBuyTransactionId(900L)).thenReturn(null);
    when(
            transactionRepository
                .findByIdempotencyKeyAndLeagueIdAndUserIdAndSeasonYearAndSeasonType(
                    "sell-null", LEAGUE_ID, USER_ID, 2026, "2"))
        .thenReturn(Optional.empty());
    when(walletRepository.findByUserIdAndLeagueIdForUpdate(USER_ID, LEAGUE_ID))
        .thenReturn(Optional.of(wallet));

    var response =
        service.processStockSell(
            USER_ID,
            LEAGUE_ID,
            new StockTransactionRequest(
                LEAGUE_ID, STOCK_ID, bd("1.0000"), null, 900L, "sell-null"));

    assertEquals("STOCK_SELL", response.type());
    assertEquals(bd("9.0000"), response.amount());
  }

  @Test
  void issueInitialStipends_createsMissingWalletAndCreditsIt() {
    Wallet wallet = wallet(USER_ID, LEAGUE_ID, "0.0000");

    stubWalletSave();
    stubTransactionSave();
    when(walletRepository.existsByUserIdAndLeagueId(USER_ID, LEAGUE_ID)).thenReturn(false);
    when(walletRepository.findByUserIdAndLeagueIdForUpdate(USER_ID, LEAGUE_ID))
        .thenReturn(Optional.of(wallet));
    when(
            transactionRepository
                .existsByIdempotencyKeyAndLeagueIdAndUserIdAndSeasonYearAndSeasonType(
                    "INITIAL_STIPEND:7:42:2026:2", LEAGUE_ID, USER_ID, 2026, "2"))
        .thenReturn(false);

    var response = service.issueInitialStipends(LEAGUE_ID, bd("250.0000"), List.of(USER_ID));

    assertEquals(1, response.walletsCreated());
    assertEquals(1, response.stipendsIssued());
    assertEquals(bd("250.0000"), wallet.getBalance());

    ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
    verify(transactionRepository).save(transactionCaptor.capture());
    Transaction saved = transactionCaptor.getValue();
    assertEquals(TransactionType.INITIAL_STIPEND, saved.getType());
    assertEquals("INITIAL_STIPEND:7:42:2026:2", saved.getIdempotencyKey());
    assertEquals(bd("250.0000"), saved.getBalanceAfter());
  }

  @Test
  void issueWeeklyStipends_isIdempotentPerLeagueUserAndWeek() {
    Wallet wallet = wallet(USER_ID, LEAGUE_ID, "250.0000");

    when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID)).thenReturn(List.of(USER_ID));
    when(
            transactionRepository
                .existsByIdempotencyKeyAndLeagueIdAndUserIdAndSeasonYearAndSeasonType(
                    "WEEKLY_STIPEND:7:42:3:2026:2", LEAGUE_ID, USER_ID, 2026, "2"))
        .thenReturn(true);

    var response = service.issueWeeklyStipends(LEAGUE_ID, bd("25.0000"), 3);

    assertEquals(0, response.walletsCreated());
    assertEquals(0, response.stipendsIssued());
    assertEquals(bd("250.0000"), wallet.getBalance());
    verify(transactionRepository, never()).save(any(Transaction.class));
    verify(walletRepository, never()).save(any(Wallet.class));
  }

  private static Wallet wallet(Long userId, Long leagueId, String balance) {
    Wallet wallet = new Wallet();
    wallet.setId(100L);
    wallet.setUserId(userId);
    wallet.setLeagueId(leagueId);
    wallet.setBalance(bd(balance));
    wallet.setCreatedAt(OffsetDateTime.parse("2026-04-06T12:00:00Z"));
    wallet.setUpdatedAt(OffsetDateTime.parse("2026-04-06T12:00:00Z"));
    return wallet;
  }

  private static Transaction buyTransaction(Wallet wallet, String pricePerShare, String amount) {
    Transaction transaction = new Transaction();
    transaction.setId(900L);
    transaction.setWallet(wallet);
    transaction.setType(TransactionType.STOCK_BUY);
    transaction.setLeagueId(wallet.getLeagueId());
    transaction.setUserId(wallet.getUserId());
    transaction.setReferenceId("stock:" + STOCK_ID);
    transaction.setPricePerShare(bd(pricePerShare));
    transaction.setAmount(bd(amount));
    transaction.setCreatedAt(OffsetDateTime.parse("2026-04-01T12:00:00Z"));
    return transaction;
  }

  private static StockResponse activeStock(String currentPrice) {
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
        Instant.parse("2026-04-06T11:55:00Z"));
  }

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
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
                wallet.setCreatedAt(OffsetDateTime.parse("2026-04-06T12:00:00Z"));
              }
              wallet.setUpdatedAt(OffsetDateTime.parse("2026-04-06T12:00:00Z"));
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
                transaction.setCreatedAt(OffsetDateTime.parse("2026-04-06T12:01:00Z"));
              }
              return transaction;
            });
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
