package com.sportstock.transaction.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.sportstock.common.dto.stock_market.StockResponse;
import com.sportstock.common.dto.transaction.StipendResultResponse;
import com.sportstock.common.dto.transaction.StockTransactionRequest;
import com.sportstock.common.dto.transaction.TransactionResponse;
import com.sportstock.common.dto.transaction.WalletResponse;
import com.sportstock.transaction.client.LeagueServiceClient;
import com.sportstock.transaction.client.StockMarketServiceClient;
import com.sportstock.transaction.entity.Transaction;
import com.sportstock.transaction.entity.Wallet;
import com.sportstock.transaction.enums.TransactionType;
import com.sportstock.transaction.exception.InsufficientFundsException;
import com.sportstock.transaction.exception.InvalidTradeRequestException;
import com.sportstock.transaction.exception.StockNotActiveException;
import com.sportstock.transaction.exception.TransactionAccessDeniedException;
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
            stockMarketServiceClient);
  }

  @Nested
  @DisplayName("Create Wallet Tests")
  class CreateWalletTests {

    @Test
    @DisplayName("Should create wallet for league member")
    void createWallet_success() {
      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(walletRepository.existsByUserIdAndLeagueId(USER_ID, LEAGUE_ID)).thenReturn(false);
      when(walletRepository.save(any(Wallet.class)))
          .thenAnswer(
              invocation -> {
                Wallet wallet = invocation.getArgument(0);
                wallet.setId(100L);
                return wallet;
              });

      WalletResponse response = service.createWallet(USER_ID, LEAGUE_ID);

      assertNotNull(response);
      assertEquals(USER_ID, response.userId());
      assertEquals(LEAGUE_ID, response.leagueId());

      ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
      verify(walletRepository).save(walletCaptor.capture());

      Wallet savedWallet = walletCaptor.getValue();
      assertEquals(USER_ID, savedWallet.getUserId());
      assertEquals(LEAGUE_ID, savedWallet.getLeagueId());
      assertEquals(BigDecimal.ZERO, savedWallet.getBalance());
    }

    @Test
    @DisplayName("Should throw exception when wallet already exists")
    void createWallet_alreadyExists() {
      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(walletRepository.existsByUserIdAndLeagueId(USER_ID, LEAGUE_ID)).thenReturn(true);

      assertThrows(
          WalletAlreadyExistsException.class, () -> service.createWallet(USER_ID, LEAGUE_ID));

      verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should throw exception when user is not a league member")
    void createWallet_notMember() {
      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID)).thenReturn(Arrays.asList(99L));

      assertThrows(
          TransactionAccessDeniedException.class, () -> service.createWallet(USER_ID, LEAGUE_ID));
    }
  }

  @Nested
  @DisplayName("Get Wallet Tests")
  class GetWalletTests {

    @Test
    @DisplayName("Should get wallet for league member")
    void getWallet_success() {
      Wallet wallet = createWallet(USER_ID, LEAGUE_ID, "500.00");

      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(walletRepository.findByUserIdAndLeagueId(USER_ID, LEAGUE_ID))
          .thenReturn(Optional.of(wallet));

      WalletResponse response = service.getWallet(USER_ID, LEAGUE_ID);

      assertNotNull(response);
      assertEquals(USER_ID, response.userId());
      assertEquals(LEAGUE_ID, response.leagueId());
      assertEquals(bd("500.00"), response.balance());
    }

    @Test
    @DisplayName("Should throw exception when wallet not found")
    void getWallet_notFound() {
      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(walletRepository.findByUserIdAndLeagueId(USER_ID, LEAGUE_ID))
          .thenReturn(Optional.empty());

      assertThrows(WalletNotFoundException.class, () -> service.getWallet(USER_ID, LEAGUE_ID));
    }
  }

  @Nested
  @DisplayName("Get League Wallets Tests")
  class GetLeagueWalletsTests {

    @Test
    @DisplayName("Should get all wallets for a league")
    void getLeagueWallets_success() {
      List<Wallet> wallets =
          Arrays.asList(
              createWallet(USER_ID, LEAGUE_ID, "500.00"),
              createWallet(43L, LEAGUE_ID, "300.00"),
              createWallet(44L, LEAGUE_ID, "700.00"));

      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(walletRepository.findAllByLeagueId(LEAGUE_ID)).thenReturn(wallets);

      List<WalletResponse> responses = service.getLeagueWallets(USER_ID, LEAGUE_ID);

      assertEquals(3, responses.size());
      assertTrue(responses.stream().allMatch(w -> w.leagueId().equals(LEAGUE_ID)));
    }
  }

  @Nested
  @DisplayName("Stock Buy Tests")
  class StockBuyTests {

    @Test
    @DisplayName("Should buy stock with quantity parameter")
    void buyStock_withQuantity_success() {
      Wallet wallet = createWallet(USER_ID, LEAGUE_ID, "1000.00");
      StockResponse stock = createActiveStock("50.00");

      setupMocksForTrade(wallet);
      when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(stock);

      TransactionResponse response =
          service.processStockBuy(
              USER_ID,
              LEAGUE_ID,
              new StockTransactionRequest(LEAGUE_ID, STOCK_ID, bd("5.0000"), null, null, "buy-1"));

      assertEquals("STOCK_BUY", response.type());
      assertEquals(bd("250.0000"), response.amount()); // 5 * 50
      assertEquals(bd("1000.00"), response.balanceBefore());
      assertEquals(
          0, bd("750.0000").compareTo(response.balanceAfter())); // Compare BigDecimals properly
      assertEquals(0, bd("50.00").compareTo(response.pricePerShare()));

      verify(walletRepository).save(argThat(w -> w.getBalance().compareTo(bd("750.00")) == 0));
    }

    @Test
    @DisplayName("Should buy stock with dollar amount parameter")
    void buyStock_withDollarAmount_success() {
      Wallet wallet = createWallet(USER_ID, LEAGUE_ID, "1000.00");
      StockResponse stock = createActiveStock("50.00");

      setupMocksForTrade(wallet);
      when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(stock);

      TransactionResponse response =
          service.processStockBuy(
              USER_ID,
              LEAGUE_ID,
              new StockTransactionRequest(LEAGUE_ID, STOCK_ID, null, bd("200.00"), null, "buy-2"));

      assertEquals("STOCK_BUY", response.type());
      assertEquals(0, bd("200.00").compareTo(response.amount())); // Use compareTo for BigDecimal
      // Quantity = 200 / 50 = 4
      assertEquals(0, bd("800.00").compareTo(response.balanceAfter()));
    }

    @Test
    @DisplayName("Should throw exception when insufficient funds")
    void buyStock_insufficientFunds() {
      Wallet wallet = createWallet(USER_ID, LEAGUE_ID, "10.00");
      StockResponse stock = createActiveStock("50.00");

      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(stock);
      when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
      when(walletRepository.findByUserIdAndLeagueIdForUpdate(USER_ID, LEAGUE_ID))
          .thenReturn(Optional.of(wallet));

      assertThrows(
          InsufficientFundsException.class,
          () ->
              service.processStockBuy(
                  USER_ID,
                  LEAGUE_ID,
                  new StockTransactionRequest(
                      LEAGUE_ID, STOCK_ID, bd("1.0000"), null, null, "buy-fail")));

      assertEquals(bd("10.00"), wallet.getBalance()); // Balance unchanged
    }

    @Test
    @DisplayName("Should throw exception when stock is not active")
    void buyStock_stockNotActive() {
      Wallet wallet = createWallet(USER_ID, LEAGUE_ID, "1000.00");
      StockResponse stock = createInactiveStock();

      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(stock);

      assertThrows(
          StockNotActiveException.class,
          () ->
              service.processStockBuy(
                  USER_ID,
                  LEAGUE_ID,
                  new StockTransactionRequest(
                      LEAGUE_ID, STOCK_ID, bd("1.0000"), null, null, "buy-inactive")));
    }

    @Test
    @DisplayName("Should throw exception when stock is game-locked")
    void buyStock_gameLocked() {
      Wallet wallet = createWallet(USER_ID, LEAGUE_ID, "1000.00");
      StockResponse stock = createGameLockedStock();

      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(stock);

      assertThrows(
          InvalidTradeRequestException.class,
          () ->
              service.processStockBuy(
                  USER_ID,
                  LEAGUE_ID,
                  new StockTransactionRequest(
                      LEAGUE_ID, STOCK_ID, bd("1.0000"), null, null, "buy-locked")));
    }

    @Test
    @DisplayName("Should throw exception when stock is injury-locked")
    void buyStock_injuryLocked() {
      Wallet wallet = createWallet(USER_ID, LEAGUE_ID, "1000.00");
      StockResponse stock = createInjuryLockedStock();

      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(stock);

      assertThrows(
          InvalidTradeRequestException.class,
          () ->
              service.processStockBuy(
                  USER_ID,
                  LEAGUE_ID,
                  new StockTransactionRequest(
                      LEAGUE_ID, STOCK_ID, bd("1.0000"), null, null, "buy-inj")));
    }

    @Test
    @DisplayName("Should handle idempotent buy requests")
    void buyStock_idempotent() {
      Wallet wallet = createWallet(USER_ID, LEAGUE_ID, "1000.00");
      Transaction existingTx = createBuyTransaction(wallet, "50.00", "250.00");

      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(stockMarketServiceClient.getStock(STOCK_ID))
          .thenReturn(createActiveStock("50.00")); // Add this mock
      when(transactionRepository.findByIdempotencyKey("buy-repeat"))
          .thenReturn(Optional.of(existingTx));

      TransactionResponse response =
          service.processStockBuy(
              USER_ID,
              LEAGUE_ID,
              new StockTransactionRequest(
                  LEAGUE_ID, STOCK_ID, bd("5.0000"), null, null, "buy-repeat"));

      assertEquals("STOCK_BUY", response.type());
      assertEquals(bd("250.00"), response.amount());

      // Should not create a new transaction
      verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should validate quantity is greater than zero")
    void buyStock_zeroQuantity() {
      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));

      assertThrows(
          InvalidTradeRequestException.class,
          () ->
              service.processStockBuy(
                  USER_ID,
                  LEAGUE_ID,
                  new StockTransactionRequest(
                      LEAGUE_ID, STOCK_ID, BigDecimal.ZERO, null, null, "buy-zero")));
    }

    @Test
    @DisplayName("Should validate either quantity or dollarAmount is provided")
    void buyStock_bothQuantityAndDollar() {
      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));

      assertThrows(
          InvalidTradeRequestException.class,
          () ->
              service.processStockBuy(
                  USER_ID,
                  LEAGUE_ID,
                  new StockTransactionRequest(
                      LEAGUE_ID, STOCK_ID, bd("5.0"), bd("100.0"), null, "buy-both")));
    }

    @Test
    @DisplayName("Should validate idempotencyKey is provided")
    void buyStock_noIdempotencyKey() {
      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));

      assertThrows(
          InvalidTradeRequestException.class,
          () ->
              service.processStockBuy(
                  USER_ID,
                  LEAGUE_ID,
                  new StockTransactionRequest(LEAGUE_ID, STOCK_ID, bd("5.0"), null, null, null)));
    }
  }

  @Nested
  @DisplayName("Stock Sell Tests")
  class StockSellTests {

    @Test
    @DisplayName("Should sell stock at 90% of buy price")
    void sellStock_success() {
      Wallet wallet = createWallet(USER_ID, LEAGUE_ID, "500.00");
      Transaction buyTransaction =
          createBuyTransaction(wallet, "50.00", "250.00"); // 5 shares @ $50
      StockResponse stock = createActiveStock("60.00"); // Current price doesn't matter

      setupMocksForTrade(wallet);
      when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(stock);
      when(transactionRepository.findById(900L)).thenReturn(Optional.of(buyTransaction));
      when(transactionRepository.sumSoldQuantityByBuyTransactionId(900L))
          .thenReturn(BigDecimal.ZERO);

      TransactionResponse response =
          service.processStockSell(
              USER_ID,
              LEAGUE_ID,
              new StockTransactionRequest(LEAGUE_ID, STOCK_ID, bd("3.0000"), null, 900L, "sell-1"));

      assertEquals("STOCK_SELL", response.type());
      // Sell price = 50 * 0.90 = 45 per share
      // Total = 3 * 45 = 135
      assertEquals(0, bd("135.0000").compareTo(response.amount())); // Use compareTo for precision
      assertEquals(0, bd("45.00").compareTo(response.pricePerShare()));
      assertEquals(bd("500.00"), response.balanceBefore());
      assertEquals(0, bd("635.00").compareTo(response.balanceAfter()));

      verify(walletRepository).save(argThat(w -> w.getBalance().compareTo(bd("635.00")) == 0));
    }

    @Test
    @DisplayName("Should throw exception when buyTransactionId is missing")
    void sellStock_noBuyTransactionId() {
      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(createActiveStock("50.00"));

      assertThrows(
          InvalidTradeRequestException.class,
          () ->
              service.processStockSell(
                  USER_ID,
                  LEAGUE_ID,
                  new StockTransactionRequest(
                      LEAGUE_ID, STOCK_ID, bd("1.0000"), null, null, "sell-no-buy")));
    }

    @Test
    @DisplayName("Should throw exception when buy transaction not found")
    void sellStock_buyTransactionNotFound() {
      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(createActiveStock("50.00"));
      when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

      assertThrows(
          InvalidTradeRequestException.class,
          () ->
              service.processStockSell(
                  USER_ID,
                  LEAGUE_ID,
                  new StockTransactionRequest(
                      LEAGUE_ID, STOCK_ID, bd("1.0000"), null, 999L, "sell-not-found")));
    }

    @Test
    @DisplayName("Should throw exception when buy transaction is not STOCK_BUY type")
    void sellStock_buyTransactionWrongType() {
      Wallet wallet = createWallet(USER_ID, LEAGUE_ID, "500.00");
      Transaction stipendTx = createStipendTransaction(wallet);

      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(createActiveStock("50.00"));
      when(transactionRepository.findById(800L)).thenReturn(Optional.of(stipendTx));

      assertThrows(
          InvalidTradeRequestException.class,
          () ->
              service.processStockSell(
                  USER_ID,
                  LEAGUE_ID,
                  new StockTransactionRequest(
                      LEAGUE_ID, STOCK_ID, bd("1.0000"), null, 800L, "sell-wrong-type")));
    }

    @Test
    @DisplayName("Should throw exception when stockId doesn't match buy transaction")
    void sellStock_wrongStock() {
      Wallet wallet = createWallet(USER_ID, LEAGUE_ID, "500.00");
      Transaction buyTransaction = createBuyTransaction(wallet, "50.00", "250.00");
      UUID differentStockId = UUID.fromString("22222222-2222-2222-2222-222222222222");

      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(stockMarketServiceClient.getStock(differentStockId))
          .thenReturn(createActiveStock("50.00", differentStockId));
      when(transactionRepository.findById(900L)).thenReturn(Optional.of(buyTransaction));

      assertThrows(
          InvalidTradeRequestException.class,
          () ->
              service.processStockSell(
                  USER_ID,
                  LEAGUE_ID,
                  new StockTransactionRequest(
                      LEAGUE_ID, differentStockId, bd("1.0000"), null, 900L, "sell-wrong-stock")));
    }

    @Test
    @DisplayName("Should throw exception when buy transaction belongs to different user")
    void sellStock_wrongUser() {
      Wallet otherWallet = createWallet(99L, LEAGUE_ID, "500.00");
      Transaction buyTransaction = createBuyTransaction(otherWallet, "50.00", "250.00");

      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(createActiveStock("50.00"));
      when(transactionRepository.findById(900L)).thenReturn(Optional.of(buyTransaction));

      assertThrows(
          InvalidTradeRequestException.class,
          () ->
              service.processStockSell(
                  USER_ID,
                  LEAGUE_ID,
                  new StockTransactionRequest(
                      LEAGUE_ID, STOCK_ID, bd("1.0000"), null, 900L, "sell-wrong-user")));
    }

    @Test
    @DisplayName("Should throw exception when sell quantity exceeds remaining shares")
    void sellStock_exceedsRemaining() {
      Wallet wallet = createWallet(USER_ID, LEAGUE_ID, "500.00");
      Transaction buyTransaction = createBuyTransaction(wallet, "50.00", "250.00"); // 5 shares

      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(createActiveStock("50.00"));
      when(transactionRepository.findById(900L)).thenReturn(Optional.of(buyTransaction));
      when(transactionRepository.sumSoldQuantityByBuyTransactionId(900L))
          .thenReturn(bd("2.0000")); // Already sold 2 shares, 3 remaining

      assertThrows(
          InvalidTradeRequestException.class,
          () ->
              service.processStockSell(
                  USER_ID,
                  LEAGUE_ID,
                  new StockTransactionRequest(
                      LEAGUE_ID, STOCK_ID, bd("4.0000"), null, 900L, "sell-too-many")));
    }

    @Test
    @DisplayName("Should allow partial sells from same buy transaction")
    void sellStock_partialSells() {
      Wallet wallet = createWallet(USER_ID, LEAGUE_ID, "500.00");
      Transaction buyTransaction =
          createBuyTransaction(wallet, "50.00", "500.00"); // 10 shares @ $50

      setupMocksForTrade(wallet);
      when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(createActiveStock("50.00"));
      when(transactionRepository.findById(900L)).thenReturn(Optional.of(buyTransaction));

      // First sell: 3 shares
      when(transactionRepository.sumSoldQuantityByBuyTransactionId(900L))
          .thenReturn(BigDecimal.ZERO);
      when(transactionRepository.findByIdempotencyKey("sell-1")).thenReturn(Optional.empty());

      TransactionResponse firstSell =
          service.processStockSell(
              USER_ID,
              LEAGUE_ID,
              new StockTransactionRequest(LEAGUE_ID, STOCK_ID, bd("3.0000"), null, 900L, "sell-1"));

      assertEquals(0, bd("135.00").compareTo(firstSell.amount())); // 3 * 45

      // Second sell: 5 more shares (8 total sold, 2 remaining)
      when(transactionRepository.sumSoldQuantityByBuyTransactionId(900L)).thenReturn(bd("3.0000"));
      when(transactionRepository.findByIdempotencyKey("sell-2")).thenReturn(Optional.empty());

      TransactionResponse secondSell =
          service.processStockSell(
              USER_ID,
              LEAGUE_ID,
              new StockTransactionRequest(LEAGUE_ID, STOCK_ID, bd("5.0000"), null, 900L, "sell-2"));

      assertEquals(0, bd("225.00").compareTo(secondSell.amount())); // 5 * 45
    }

    @Test
    @DisplayName("Should throw exception when stock is game-locked")
    void sellStock_gameLocked() {
      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(stockMarketServiceClient.getStock(STOCK_ID)).thenReturn(createGameLockedStock());

      assertThrows(
          InvalidTradeRequestException.class,
          () ->
              service.processStockSell(
                  USER_ID,
                  LEAGUE_ID,
                  new StockTransactionRequest(
                      LEAGUE_ID, STOCK_ID, bd("1.0000"), null, 900L, "sell-locked")));
    }
  }

  @Nested
  @DisplayName("Initial Stipend Tests")
  class InitialStipendTests {

    @Test
    @DisplayName("Should issue initial stipends to all members")
    void issueInitialStipends_toAllMembers() {
      List<Long> userIds = Arrays.asList(USER_ID, 43L, 44L);

      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID)).thenReturn(userIds);

      for (Long userId : userIds) {
        when(walletRepository.existsByUserIdAndLeagueId(userId, LEAGUE_ID)).thenReturn(false);
        when(walletRepository.findByUserIdAndLeagueIdForUpdate(userId, LEAGUE_ID))
            .thenReturn(Optional.of(createWallet(userId, LEAGUE_ID, "0.00")));
        when(transactionRepository.existsByIdempotencyKey(
                "INITIAL_STIPEND:" + LEAGUE_ID + ":" + userId))
            .thenReturn(false);
      }

      stubWalletSave();
      stubTransactionSave();

      StipendResultResponse response = service.issueInitialStipends(LEAGUE_ID, bd("1000.00"), null);

      assertEquals(LEAGUE_ID, response.leagueId());
      assertEquals(3, response.walletsCreated());
      assertEquals(3, response.stipendsIssued());
      assertEquals(bd("1000.00"), response.amountPerUser());
    }

    @Test
    @DisplayName("Should issue stipends to specific users")
    void issueInitialStipends_toSpecificUsers() {
      List<Long> specificUsers = Arrays.asList(USER_ID, 43L);

      for (Long userId : specificUsers) {
        when(walletRepository.existsByUserIdAndLeagueId(userId, LEAGUE_ID)).thenReturn(false);
        when(walletRepository.findByUserIdAndLeagueIdForUpdate(userId, LEAGUE_ID))
            .thenReturn(Optional.of(createWallet(userId, LEAGUE_ID, "0.00")));
        when(transactionRepository.existsByIdempotencyKey(
                "INITIAL_STIPEND:" + LEAGUE_ID + ":" + userId))
            .thenReturn(false);
      }

      stubWalletSave();
      stubTransactionSave();

      StipendResultResponse response =
          service.issueInitialStipends(LEAGUE_ID, bd("1000.00"), specificUsers);

      assertEquals(2, response.walletsCreated());
      assertEquals(2, response.stipendsIssued());
    }

    @Test
    @DisplayName("Should be idempotent and not issue duplicate stipends")
    void issueInitialStipends_idempotent() {
      when(walletRepository.existsByUserIdAndLeagueId(USER_ID, LEAGUE_ID)).thenReturn(true);
      when(transactionRepository.existsByIdempotencyKey(
              "INITIAL_STIPEND:" + LEAGUE_ID + ":" + USER_ID))
          .thenReturn(true);

      StipendResultResponse response =
          service.issueInitialStipends(LEAGUE_ID, bd("1000.00"), Arrays.asList(USER_ID));

      assertEquals(0, response.walletsCreated());
      assertEquals(0, response.stipendsIssued());
    }

    @Test
    @DisplayName("Should create wallet if it doesn't exist")
    void issueInitialStipends_createsWallet() {
      when(walletRepository.existsByUserIdAndLeagueId(USER_ID, LEAGUE_ID)).thenReturn(false);
      when(walletRepository.findByUserIdAndLeagueIdForUpdate(USER_ID, LEAGUE_ID))
          .thenReturn(Optional.of(createWallet(USER_ID, LEAGUE_ID, "0.00")));
      when(transactionRepository.existsByIdempotencyKey(
              "INITIAL_STIPEND:" + LEAGUE_ID + ":" + USER_ID))
          .thenReturn(false);

      stubWalletSave();
      stubTransactionSave();

      StipendResultResponse response =
          service.issueInitialStipends(LEAGUE_ID, bd("1000.00"), Arrays.asList(USER_ID));

      assertEquals(1, response.walletsCreated());
      assertEquals(1, response.stipendsIssued());
      verify(walletRepository, atLeastOnce()).save(any(Wallet.class));
    }
  }

  @Nested
  @DisplayName("Weekly Stipend Tests")
  class WeeklyStipendTests {

    @Test
    @DisplayName("Should issue weekly stipends to all members")
    void issueWeeklyStipends_success() {
      List<Long> userIds = Arrays.asList(USER_ID, 43L, 44L);

      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID)).thenReturn(userIds);

      for (Long userId : userIds) {
        when(walletRepository.findByUserIdAndLeagueIdForUpdate(userId, LEAGUE_ID))
            .thenReturn(Optional.of(createWallet(userId, LEAGUE_ID, "500.00")));
        when(transactionRepository.existsByIdempotencyKey(
                "WEEKLY_STIPEND:" + LEAGUE_ID + ":" + userId + ":5"))
            .thenReturn(false);
      }

      stubWalletSave();
      stubTransactionSave();

      StipendResultResponse response = service.issueWeeklyStipends(LEAGUE_ID, bd("100.00"), 5);

      assertEquals(LEAGUE_ID, response.leagueId());
      assertEquals(0, response.walletsCreated()); // Wallets already exist
      assertEquals(3, response.stipendsIssued());
      assertEquals(bd("100.00"), response.amountPerUser());
    }

    @Test
    @DisplayName("Should be idempotent per league, user, and week")
    void issueWeeklyStipends_idempotent() {
      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(transactionRepository.existsByIdempotencyKey(
              "WEEKLY_STIPEND:" + LEAGUE_ID + ":" + USER_ID + ":3"))
          .thenReturn(true);

      StipendResultResponse response = service.issueWeeklyStipends(LEAGUE_ID, bd("100.00"), 3);

      assertEquals(0, response.stipendsIssued());
      verify(walletRepository, never()).save(any(Wallet.class));
      verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should allow issuing same amount for different weeks")
    void issueWeeklyStipends_differentWeeks() {
      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(walletRepository.findByUserIdAndLeagueIdForUpdate(USER_ID, LEAGUE_ID))
          .thenReturn(Optional.of(createWallet(USER_ID, LEAGUE_ID, "500.00")));
      when(transactionRepository.existsByIdempotencyKey(anyString())).thenReturn(false);

      stubWalletSave();
      stubTransactionSave();

      // Week 3
      StipendResultResponse week3 = service.issueWeeklyStipends(LEAGUE_ID, bd("100.00"), 3);
      assertEquals(1, week3.stipendsIssued());

      // Week 4 should also work
      StipendResultResponse week4 = service.issueWeeklyStipends(LEAGUE_ID, bd("100.00"), 4);
      assertEquals(1, week4.stipendsIssued());
    }
  }

  @Nested
  @DisplayName("Transaction History Tests")
  class TransactionHistoryTests {

    @Test
    @DisplayName("Should get paginated transaction history")
    void getTransactionHistory_success() {
      Wallet wallet = createWallet(USER_ID, LEAGUE_ID, "500.00");
      List<Transaction> transactions =
          Arrays.asList(
              createBuyTransaction(wallet, "50.00", "250.00"), createStipendTransaction(wallet));
      Page<Transaction> page = new PageImpl<>(transactions);

      when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
          .thenReturn(Arrays.asList(USER_ID));
      when(transactionRepository.findByUserIdAndLeagueIdOrderByCreatedAtDesc(
              eq(USER_ID), eq(LEAGUE_ID), any(PageRequest.class)))
          .thenReturn(page);

      Page<TransactionResponse> result =
          service.getTransactionHistory(USER_ID, LEAGUE_ID, PageRequest.of(0, 10));

      assertEquals(2, result.getContent().size());
    }
  }

  // Helper methods
  private void setupMocksForTrade(Wallet wallet) {
    when(leagueServiceClient.getMemberUserIdsInternal(LEAGUE_ID))
        .thenReturn(Arrays.asList(USER_ID));
    when(transactionRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
    when(walletRepository.findByUserIdAndLeagueIdForUpdate(USER_ID, LEAGUE_ID))
        .thenReturn(Optional.of(wallet));
    stubWalletSave();
    stubTransactionSave();
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

  private Wallet createWallet(Long userId, Long leagueId, String balance) {
    Wallet wallet = new Wallet();
    wallet.setId(100L);
    wallet.setUserId(userId);
    wallet.setLeagueId(leagueId);
    wallet.setBalance(bd(balance));
    wallet.setCreatedAt(OffsetDateTime.now());
    wallet.setUpdatedAt(OffsetDateTime.now());
    return wallet;
  }

  private Transaction createBuyTransaction(Wallet wallet, String pricePerShare, String amount) {
    Transaction transaction = new Transaction();
    transaction.setId(900L);
    transaction.setWallet(wallet);
    transaction.setType(TransactionType.STOCK_BUY);
    transaction.setLeagueId(wallet.getLeagueId());
    transaction.setUserId(wallet.getUserId());
    transaction.setReferenceId("stock:" + STOCK_ID);
    transaction.setPricePerShare(bd(pricePerShare));
    transaction.setAmount(bd(amount));
    transaction.setBalanceBefore(bd("1000.00"));
    transaction.setBalanceAfter(bd("750.00"));
    transaction.setCreatedAt(OffsetDateTime.now());
    return transaction;
  }

  private Transaction createStipendTransaction(Wallet wallet) {
    Transaction transaction = new Transaction();
    transaction.setId(800L);
    transaction.setWallet(wallet);
    transaction.setType(TransactionType.INITIAL_STIPEND);
    transaction.setLeagueId(wallet.getLeagueId());
    transaction.setUserId(wallet.getUserId());
    transaction.setAmount(bd("1000.00"));
    transaction.setBalanceBefore(bd("0.00"));
    transaction.setBalanceAfter(bd("1000.00"));
    transaction.setIdempotencyKey("INITIAL_STIPEND:" + LEAGUE_ID + ":" + USER_ID);
    transaction.setCreatedAt(OffsetDateTime.now());
    return transaction;
  }

  private StockResponse createActiveStock(String currentPrice) {
    return createActiveStock(currentPrice, STOCK_ID);
  }

  private StockResponse createActiveStock(String currentPrice, UUID stockId) {
    return new StockResponse(
        stockId,
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

  private StockResponse createInactiveStock() {
    return new StockResponse(
        STOCK_ID,
        "3139477",
        "Patrick Mahomes",
        "QB",
        "PLAYER",
        "12",
        bd("50.00"),
        "INACTIVE",
        false,
        false,
        Instant.now());
  }

  private StockResponse createGameLockedStock() {
    return new StockResponse(
        STOCK_ID,
        "3139477",
        "Patrick Mahomes",
        "QB",
        "PLAYER",
        "12",
        bd("50.00"),
        "ACTIVE",
        true, // game locked
        false,
        Instant.now());
  }

  private StockResponse createInjuryLockedStock() {
    return new StockResponse(
        STOCK_ID,
        "3139477",
        "Patrick Mahomes",
        "QB",
        "PLAYER",
        "12",
        bd("50.00"),
        "ACTIVE",
        false,
        true, // injury locked
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
