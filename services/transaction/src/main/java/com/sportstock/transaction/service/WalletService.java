package com.sportstock.transaction.service;

import com.sportstock.common.dto.transaction.StipendResultResponse;
import com.sportstock.common.dto.transaction.TransactionResponse;
import com.sportstock.common.dto.transaction.WalletResponse;
import com.sportstock.transaction.client.LeagueServiceClient;
import com.sportstock.transaction.entity.Transaction;
import com.sportstock.transaction.entity.Wallet;
import com.sportstock.transaction.enums.TransactionType;
import com.sportstock.transaction.exception.WalletAlreadyExistsException;
import com.sportstock.transaction.exception.WalletNotFoundException;
import com.sportstock.transaction.mapper.DtoMapper;
import com.sportstock.transaction.repo.TransactionRepository;
import com.sportstock.transaction.repo.WalletRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class WalletService {

  private final WalletRepository walletRepository;
  private final TransactionRepository transactionRepository;
  private final TransactionTemplate transactionTemplate;
  private final LeagueServiceClient leagueServiceClient;

  public WalletService(
      WalletRepository walletRepository,
      TransactionRepository transactionRepository,
      PlatformTransactionManager txManager,
      LeagueServiceClient leagueServiceClient) {
    this.walletRepository = walletRepository;
    this.transactionRepository = transactionRepository;
    this.transactionTemplate = new TransactionTemplate(txManager);
    this.leagueServiceClient = leagueServiceClient;
  }

  @Transactional
  public WalletResponse createWallet(Long userId, Long leagueId) {
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

  public StipendResultResponse issueInitialStipends(Long leagueId, BigDecimal amount) {
    List<Long> userIds = leagueServiceClient.getMemberUserIds(leagueId);
    AtomicInteger walletsCreated = new AtomicInteger(0);
    AtomicInteger stipendsIssued = new AtomicInteger(0);

    for (Long userId : userIds) {
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

            BigDecimal balanceBefore = wallet.getBalance();
            BigDecimal balanceAfter = balanceBefore.add(amount);

            Transaction transaction = new Transaction();
            transaction.setWallet(wallet);
            transaction.setType(TransactionType.INITIAL_STIPEND);
            transaction.setAmount(amount);
            transaction.setBalanceBefore(balanceBefore);
            transaction.setBalanceAfter(balanceAfter);
            transaction.setLeagueId(leagueId);
            transaction.setUserId(userId);
            transaction.setIdempotencyKey(idempotencyKey);

            try {
              transactionRepository.save(transaction);
            } catch (DataIntegrityViolationException e) {
              status.setRollbackOnly();
              return;
            }

            wallet.setBalance(balanceAfter);
            stipendsIssued.incrementAndGet();
          });
    }

    return new StipendResultResponse(leagueId, walletsCreated.get(), stipendsIssued.get(), amount);
  }

  public StipendResultResponse issueWeeklyStipends(
      Long leagueId, BigDecimal amount, Integer weekNumber) {
    List<Long> userIds = leagueServiceClient.getMemberUserIds(leagueId);
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
            BigDecimal balanceBefore = wallet.getBalance();
            BigDecimal balanceAfter = balanceBefore.add(amount);

            Transaction transaction = new Transaction();
            transaction.setWallet(wallet);
            transaction.setType(TransactionType.WEEKLY_STIPEND);
            transaction.setAmount(amount);
            transaction.setBalanceBefore(balanceBefore);
            transaction.setBalanceAfter(balanceAfter);
            transaction.setLeagueId(leagueId);
            transaction.setUserId(userId);
            transaction.setIdempotencyKey(idempotencyKey);

            try {
              transactionRepository.save(transaction);
            } catch (DataIntegrityViolationException e) {
              status.setRollbackOnly();
              return;
            }

            wallet.setBalance(balanceAfter);
            stipendsIssued.incrementAndGet();
          });
    }
    return new StipendResultResponse(leagueId, 0, stipendsIssued.get(), amount);
  }

  @Transactional
  public TransactionResponse processStockBuy(
      Long userId, Long leagueId, BigDecimal amount, String referenceId, String description) {
    // TODO: Implement stock buy (debit)
    // - Lock wallet (findByUserIdAndLeagueIdForUpdate)
    // - Check if wallet exists, throw WalletNotFoundException if not
    // - Check if balance >= amount, throw InsufficientFundsException if not
    // - Debit balance (subtract amount)
    // - Create Transaction record with type STOCK_BUY
    // - Return TransactionResponse
    throw new UnsupportedOperationException("TODO: Implement processStockBuy");
  }

  @Transactional
  public TransactionResponse processStockSell(
      Long userId, Long leagueId, BigDecimal amount, String referenceId, String description) {
    // TODO: Implement stock sell (credit)
    // - Lock wallet (findByUserIdAndLeagueIdForUpdate)
    // - Check if wallet exists, throw WalletNotFoundException if not
    // - Credit balance (add amount)
    // - Create Transaction record with type STOCK_SELL
    // - Return TransactionResponse
    throw new UnsupportedOperationException("TODO: Implement processStockSell");
  }

  @Transactional(readOnly = true)
  public WalletResponse getWallet(Long userId, Long leagueId) {
    return DtoMapper.toWalletResponse(
        walletRepository
            .findByUserIdAndLeagueId(userId, leagueId)
            .orElseThrow(
                () -> new WalletNotFoundException("Wallet not found for user: " + userId)));
  }

  @Transactional(readOnly = true)
  public Page<TransactionResponse> getTransactionHistory(
      Long userId, Long leagueId, Pageable pageable) {
    return transactionRepository
        .findByUserIdAndLeagueIdOrderByCreatedAtDesc(userId, leagueId, pageable)
        .map(DtoMapper::toTransactionResponse);
  }

  @Transactional(readOnly = true)
  public List<WalletResponse> getLeagueWallets(Long leagueId) {
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
}
