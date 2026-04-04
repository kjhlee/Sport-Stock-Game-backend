package com.sportstock.transaction.service;

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
import com.sportstock.transaction.mapper.DtoMapper;
import com.sportstock.transaction.repo.TransactionRepository;
import com.sportstock.transaction.repo.WalletRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class WalletService {

  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final TransactionTemplate transactionTemplate;
  private final LeagueServiceClient leagueServiceClient;
  private final StockMarketServiceClient stockMarketServiceClient;

  public WalletService(
      WalletRepository walletRepository,
      TransactionRepository transactionRepository,
      PlatformTransactionManager txManager,
      LeagueServiceClient leagueServiceClient,
      StockMarketServiceClient stockMarketServiceClient) {
    this.walletRepository = walletRepository;
    this.transactionRepository = transactionRepository;
    this.transactionTemplate = new TransactionTemplate(txManager);
    this.leagueServiceClient = leagueServiceClient;
    this.stockMarketServiceClient = stockMarketServiceClient;
  }

  @Transactional
  public WalletResponse createWallet(Long userId, Long leagueId) {
    verifyLeagueMembership(userId, leagueId);
    if (walletRepository.existsByUserIdAndLeagueId(userId, leagueId)) {
      throw new WalletAlreadyExistsException(userId, leagueId);
    }
    Wallet wallet = new Wallet();
    wallet.setUserId(userId);
    wallet.setLeagueId(leagueId);
    try {
      walletRepository.save(wallet);
    } catch (DataIntegrityViolationException e) {
      throw new WalletAlreadyExistsException(userId, leagueId);
    }
    return DtoMapper.toWalletResponse(wallet);
  }

  public StipendResultResponse issueInitialStipends(
      Long leagueId, BigDecimal amount, List<Long> userIds) {
    List<Long> stipendUserIds =
        (userIds != null && !userIds.isEmpty())
            ? userIds
            : leagueServiceClient.getMemberUserIdsInternal(leagueId);
    AtomicInteger walletsCreated = new AtomicInteger(0);
    AtomicInteger stipendsIssued = new AtomicInteger(0);

    for (Long userId : stipendUserIds) {
      transactionTemplate.executeWithoutResult(
          status -> {
            ensureWalletExists(userId, leagueId, walletsCreated);

            String idempotencyKey = "INITIAL_STIPEND:" + leagueId + ":" + userId;
            if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
              return;
            }

            Wallet wallet =
                walletRepository
                    .findByUserIdAndLeagueIdForUpdate(userId, leagueId)
                    .orElseThrow(
                        () -> new WalletNotFoundException("Wallet not found for user: " + userId));

            try {
              creditWallet(
                  wallet,
                  amount,
                  TransactionType.INITIAL_STIPEND,
                  null,
                  null,
                  idempotencyKey,
                  null,
                  null);
            } catch (DataIntegrityViolationException e) {
              status.setRollbackOnly();
              return;
            }
            stipendsIssued.incrementAndGet();
          });
    }

    return new StipendResultResponse(leagueId, walletsCreated.get(), stipendsIssued.get(), amount);
  }

  public StipendResultResponse issueWeeklyStipends(
      Long leagueId, BigDecimal amount, Integer weekNumber) {
    List<Long> userIds = leagueServiceClient.getMemberUserIdsInternal(leagueId);
    AtomicInteger stipendsIssued = new AtomicInteger(0);

    for (Long userId : userIds) {
      transactionTemplate.executeWithoutResult(
          status -> {
            String idempotencyKey = "WEEKLY_STIPEND:" + leagueId + ":" + userId + ":" + weekNumber;

            if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
              return;
            }

            Wallet wallet =
                walletRepository
                    .findByUserIdAndLeagueIdForUpdate(userId, leagueId)
                    .orElseThrow(
                        () -> new WalletNotFoundException("Wallet not found for user: " + userId));

            try {
              creditWallet(
                  wallet,
                  amount,
                  TransactionType.WEEKLY_STIPEND,
                  null,
                  null,
                  idempotencyKey,
                  null,
                  null);
            } catch (DataIntegrityViolationException e) {
              status.setRollbackOnly();
              return;
            }
            stipendsIssued.incrementAndGet();
          });
    }
    return new StipendResultResponse(leagueId, 0, stipendsIssued.get(), amount);
  }

  public TransactionResponse processStockBuy(
      Long userId, Long leagueId, StockTransactionRequest request) {
    verifyLeagueMembership(userId, leagueId);
    validateTradeRequest(request);
    StockResponse stock = stockMarketServiceClient.getStock(request.stockId());
    validateStockActive(stock);

    if (stock.gameLocked()) {
      throw new InvalidTradeRequestException("Stock is game-locked and cannot be purchased");
    }
    if (stock.injuryLocked()) {
      throw new InvalidTradeRequestException("Stock is injury-locked and cannot be purchased");
    }

    BigDecimal pricePerShare = stock.currentPrice();
    BigDecimal quantity = resolveQuantity(request, pricePerShare);
    BigDecimal totalAmount = quantity.multiply(pricePerShare).setScale(4, RoundingMode.DOWN);

    AtomicReference<Transaction> result = new AtomicReference<>();
    transactionTemplate.executeWithoutResult(
        status -> {
          Transaction existingTransaction =
              findExistingTransaction(request.idempotencyKey(), userId, leagueId);
          if (existingTransaction != null) {
            validateReplayableTransaction(existingTransaction, TransactionType.STOCK_BUY, request);
            result.set(existingTransaction);
            return;
          }

          Wallet wallet =
              walletRepository
                  .findByUserIdAndLeagueIdForUpdate(userId, leagueId)
                  .orElseThrow(
                      () -> new WalletNotFoundException("Wallet not found for user: " + userId));

          String description =
              String.format("Buy %s shares of %s @ %s", quantity, stock.fullName(), pricePerShare);

          try {
            result.set(
                debitWallet(
                    wallet,
                    totalAmount,
                    TransactionType.STOCK_BUY,
                    "stock:" + request.stockId(),
                    description,
                    request.idempotencyKey(),
                    pricePerShare,
                    request.buyTransactionId()));
            // TODO: call portfolio service to update
          } catch (DataIntegrityViolationException e) {
            Transaction replayTransaction =
                loadRequiredTransaction(request.idempotencyKey(), userId, leagueId);
            validateReplayableTransaction(replayTransaction, TransactionType.STOCK_BUY, request);
            result.set(replayTransaction);
            status.setRollbackOnly();
          }
        });

    return DtoMapper.toTransactionResponse(result.get());
  }

  public TransactionResponse processStockSell(
      Long userId, Long leagueId, StockTransactionRequest request) {
    verifyLeagueMembership(userId, leagueId);
    validateTradeRequest(request);
    StockResponse stock = stockMarketServiceClient.getStock(request.stockId());
    validateStockActive(stock);

    if (stock.gameLocked()) {
      throw new InvalidTradeRequestException("Stock is game-locked and cannot be sold");
    }

    Long transactionId = request.buyTransactionId();
    if (transactionId == null) {
      throw new InvalidTradeRequestException("buyTransactionId is required for sell transactions");
    }

    Transaction buyTransaction =
        transactionRepository
            .findById(transactionId)
            .orElseThrow(
                () ->
                    new InvalidTradeRequestException(
                        "Buy transaction not found: " + transactionId));

    if (buyTransaction.getType() != TransactionType.STOCK_BUY) {
      throw new InvalidTradeRequestException(
          "buyTransactionId must reference a stock buy transaction");
    }

    String expectedReferenceId = "stock:" + request.stockId();
    if (!expectedReferenceId.equals(buyTransaction.getReferenceId())) {
      throw new InvalidTradeRequestException(
          "buyTransactionId does not belong to the requested stock");
    }

    if (!buyTransaction.getLeagueId().equals(leagueId)
        || !buyTransaction.getUserId().equals(userId)) {
      throw new InvalidTradeRequestException(
          "buyTransactionId does not belong to this user and league");
    }

    BigDecimal pricePerShare =
        buyTransaction
            .getPricePerShare()
            .multiply(new BigDecimal("0.90"))
            .setScale(4, RoundingMode.DOWN);

    BigDecimal quantity = resolveQuantity(request, pricePerShare);
    BigDecimal purchasedQuantity =
        buyTransaction.getAmount().divide(buyTransaction.getPricePerShare(), 4, RoundingMode.DOWN);
    BigDecimal soldQuantity =
        transactionRepository.sumSoldQuantityByBuyTransactionId(buyTransaction.getId());
    BigDecimal remainingQuantity =
        purchasedQuantity.subtract(soldQuantity).setScale(4, RoundingMode.DOWN);

    if (quantity.compareTo(remainingQuantity) > 0) {
      throw new InvalidTradeRequestException(
          "Requested sell quantity exceeds remaining shares for this buy transaction");
    }

    BigDecimal totalCredit = quantity.multiply(pricePerShare).setScale(4, RoundingMode.DOWN);

    AtomicReference<Transaction> result = new AtomicReference<>();
    transactionTemplate.executeWithoutResult(
        status -> {
          Transaction existingTransaction =
              findExistingTransaction(request.idempotencyKey(), userId, leagueId);
          if (existingTransaction != null) {
            validateReplayableTransaction(existingTransaction, TransactionType.STOCK_SELL, request);
            result.set(existingTransaction);
            return;
          }

          Wallet wallet =
              walletRepository
                  .findByUserIdAndLeagueIdForUpdate(userId, leagueId)
                  .orElseThrow(
                      () -> new WalletNotFoundException("Wallet not found for user: " + userId));

          String description =
              String.format("Sell %s shares of %s @ %s", quantity, stock.fullName(), pricePerShare);

          try {
            result.set(
                creditWallet(
                    wallet,
                    totalCredit,
                    TransactionType.STOCK_SELL,
                    "stock:" + request.stockId(),
                    description,
                    request.idempotencyKey(),
                    pricePerShare,
                    request.buyTransactionId()));
            // TODO: call portfolio service to update
          } catch (DataIntegrityViolationException e) {
            Transaction replayTransaction =
                loadRequiredTransaction(request.idempotencyKey(), userId, leagueId);
            validateReplayableTransaction(replayTransaction, TransactionType.STOCK_SELL, request);
            result.set(replayTransaction);
            status.setRollbackOnly();
          }
        });
    return DtoMapper.toTransactionResponse(result.get());
  }

  @Transactional(readOnly = true)
  public WalletResponse getWallet(Long userId, Long leagueId) {
    verifyLeagueMembership(userId, leagueId);
    return DtoMapper.toWalletResponse(
        walletRepository
            .findByUserIdAndLeagueId(userId, leagueId)
            .orElseThrow(
                () -> new WalletNotFoundException("Wallet not found for user: " + userId)));
  }

  @Transactional(readOnly = true)
  public Page<TransactionResponse> getTransactionHistory(
      Long userId, Long leagueId, Pageable pageable) {
    verifyLeagueMembership(userId, leagueId);
    return transactionRepository
        .findByUserIdAndLeagueIdOrderByCreatedAtDesc(userId, leagueId, pageable)
        .map(DtoMapper::toTransactionResponse);
  }

  @Transactional(readOnly = true)
  public List<WalletResponse> getLeagueWallets(Long userId, Long leagueId) {
    verifyLeagueMembership(userId, leagueId);
    return walletRepository.findAllByLeagueId(leagueId).stream()
        .map(DtoMapper::toWalletResponse)
        .toList();
  }

  private void ensureWalletExists(Long userId, Long leagueId, AtomicInteger walletsCreated) {
    if (walletRepository.existsByUserIdAndLeagueId(userId, leagueId)) {
      return;
    }
    Wallet wallet = new Wallet();
    wallet.setUserId(userId);
    wallet.setLeagueId(leagueId);
    try {
      walletRepository.save(wallet);
      walletsCreated.incrementAndGet();
    } catch (DataIntegrityViolationException e) {
      // Another thread created it concurrently — that's fine
    }
  }

  private void validateTradeRequest(StockTransactionRequest request) {
    if (request.stockId() == null) {
      throw new InvalidTradeRequestException("stockId is required");
    }
    if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
      throw new InvalidTradeRequestException("idempotencyKey is required");
    }
    boolean hasQuantity = request.quantity() != null;
    boolean hasDollarAmount = request.dollarAmount() != null;
    if (hasQuantity == hasDollarAmount) {
      throw new InvalidTradeRequestException(
          "Exactly one of 'quantity' or 'dollarAmount' must be provided");
    }
    if (hasQuantity && request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidTradeRequestException("Quantity must be greater than zero");
    }
    if (hasDollarAmount && request.dollarAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidTradeRequestException("Dollar amount must be greater than zero");
    }
  }

  private void validateStockActive(StockResponse stock) {
    if (!"ACTIVE".equals(stock.status())) {
      throw new StockNotActiveException(stock.id().toString(), stock.status());
    }
  }

  private BigDecimal resolveQuantity(StockTransactionRequest request, BigDecimal pricePerShare) {
    if (request.quantity() != null) {
      return request.quantity().setScale(4, RoundingMode.DOWN);
    }
    if (pricePerShare == null || pricePerShare.compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidTradeRequestException("Stock price is unavailable or invalid");
    }
    BigDecimal quantity = request.dollarAmount().divide(pricePerShare, 4, RoundingMode.DOWN);
    if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidTradeRequestException(
          "Dollar amount too small to purchase any shares at current price");
    }
    return quantity;
  }

  private Transaction creditWallet(
      Wallet wallet,
      BigDecimal amount,
      TransactionType type,
      String referenceId,
      String description,
      String idempotencyKey,
      BigDecimal pricePerShare,
      Long buyTransactionId) {
    BigDecimal balanceBefore = wallet.getBalance();
    BigDecimal balanceAfter = balanceBefore.add(amount);

    Transaction transaction = new Transaction();
    transaction.setWallet(wallet);
    transaction.setType(type);
    transaction.setAmount(amount);
    transaction.setBalanceBefore(balanceBefore);
    transaction.setBalanceAfter(balanceAfter);
    transaction.setLeagueId(wallet.getLeagueId());
    transaction.setUserId(wallet.getUserId());
    transaction.setReferenceId(referenceId);
    transaction.setDescription(description);
    transaction.setIdempotencyKey(idempotencyKey);
    transaction.setPricePerShare(pricePerShare);
    transaction.setBuyTransactionId(buyTransactionId);

    transactionRepository.save(transaction);
    wallet.setBalance(balanceAfter);
    walletRepository.save(wallet);
    return transaction;
  }

  private Transaction debitWallet(
      Wallet wallet,
      BigDecimal amount,
      TransactionType type,
      String referenceId,
      String description,
      String idempotencyKey,
      BigDecimal pricePerShare,
      Long buyTransactionId) {
    if (wallet.getBalance().compareTo(amount) < 0) {
      throw new InsufficientFundsException(wallet.getBalance(), amount);
    }

    BigDecimal balanceBefore = wallet.getBalance();
    BigDecimal balanceAfter = balanceBefore.subtract(amount);

    Transaction transaction = new Transaction();
    transaction.setWallet(wallet);
    transaction.setType(type);
    transaction.setAmount(amount);
    transaction.setBalanceBefore(balanceBefore);
    transaction.setBalanceAfter(balanceAfter);
    transaction.setLeagueId(wallet.getLeagueId());
    transaction.setUserId(wallet.getUserId());
    transaction.setReferenceId(referenceId);
    transaction.setDescription(description);
    transaction.setIdempotencyKey(idempotencyKey);
    transaction.setPricePerShare(pricePerShare);
    transaction.setBuyTransactionId(buyTransactionId);

    transactionRepository.save(transaction);
    wallet.setBalance(balanceAfter);
    walletRepository.save(wallet);
    return transaction;
  }

  public StipendResultResponse liquidateAssets(Long leagueId, int weekNumber) {
    // TODO: Implement liquidation logic
    return new StipendResultResponse(leagueId, 0, 0, BigDecimal.ZERO);
  }

  private Transaction findExistingTransaction(String idempotencyKey, Long userId, Long leagueId) {
    return transactionRepository
        .findByIdempotencyKey(idempotencyKey)
        .filter(tx -> tx.getUserId().equals(userId) && tx.getLeagueId().equals(leagueId))
        .orElse(null);
  }

  private Transaction loadRequiredTransaction(String idempotencyKey, Long userId, Long leagueId) {
    Transaction existingTransaction =
        transactionRepository
            .findByIdempotencyKey(idempotencyKey)
            .orElseThrow(
                () ->
                    new InvalidTradeRequestException(
                        "Trade replay failed for idempotency key " + idempotencyKey));
    if (!existingTransaction.getUserId().equals(userId)
        || !existingTransaction.getLeagueId().equals(leagueId)) {
      throw new InvalidTradeRequestException(
          "Idempotency key is already in use by another trade context");
    }
    return existingTransaction;
  }

  private void validateReplayableTransaction(
      Transaction existingTransaction,
      TransactionType expectedType,
      StockTransactionRequest request) {
    if (existingTransaction.getType() != expectedType) {
      throw new InvalidTradeRequestException(
          "Idempotency key already belongs to a different transaction type");
    }

    String expectedReferenceId = "stock:" + request.stockId();
    if (!expectedReferenceId.equals(existingTransaction.getReferenceId())) {
      throw new InvalidTradeRequestException(
          "Idempotency key already belongs to a different stock trade");
    }

    Long existingBuyTransactionId = existingTransaction.getBuyTransactionId();
    Long requestedBuyTransactionId = request.buyTransactionId();
    if ((existingBuyTransactionId == null && requestedBuyTransactionId != null)
        || (existingBuyTransactionId != null
            && !existingBuyTransactionId.equals(requestedBuyTransactionId))) {
      throw new InvalidTradeRequestException(
          "Idempotency key already belongs to a different buyTransactionId");
    }
  }

  private void verifyLeagueMembership(Long userId, Long leagueId) {
    boolean isMember =
        leagueServiceClient.getMemberUserIdsInternal(leagueId).stream().anyMatch(userId::equals);
    if (!isMember) {
      throw new TransactionAccessDeniedException(
          "User is not a member of league " + leagueId);
    }
  }
}
