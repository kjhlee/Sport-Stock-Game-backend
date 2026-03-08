package com.sportstock.transaction.service;

import com.sportstock.transaction.dto.response.StipendResultResponse;
import com.sportstock.transaction.dto.response.TransactionResponse;
import com.sportstock.transaction.dto.response.WalletResponse;
import com.sportstock.transaction.entity.Transaction;
import com.sportstock.transaction.entity.Wallet;
import com.sportstock.transaction.enums.TransactionType;
import com.sportstock.transaction.exception.WalletAlreadyExistsException;
import com.sportstock.transaction.exception.WalletNotFoundException;
import com.sportstock.transaction.repo.TransactionRepository;
import com.sportstock.transaction.repo.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public WalletResponse createWallet(Long userId, Long leagueId) {
        if (walletRepository.existsByUserIdAndLeagueId(userId, leagueId)) {
            throw new WalletAlreadyExistsException(userId, leagueId);
        }
        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setLeagueId(leagueId);
        walletRepository.save(wallet);

        return WalletResponse.from(wallet);

    }

    @Transactional
    public StipendResultResponse issueInitialStipends(Long leagueId, BigDecimal amount, List<Long> userIds) {
        int walletsCreated = 0;
        int stipendsIssued = 0;

        for (Long userId : userIds) {
            if (!walletRepository.existsByUserIdAndLeagueId(userId, leagueId)) {
                createWallet(userId, leagueId);
                walletsCreated++;
            }

            String idempotencyKey = "INITIAL_STIPEND:" + leagueId + ":" + userId;
            if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
                continue;
            }

            Wallet wallet = walletRepository.findByUserIdAndLeagueIdForUpdate(userId, leagueId).orElseThrow(
                    () -> new WalletNotFoundException("Wallet not found for user: " + userId)
            );

            BigDecimal balanceBefore = wallet.getBalance();
            wallet.setBalance(balanceBefore.add(amount));

            Transaction transaction = new Transaction();
            transaction.setWallet(wallet);
            transaction.setType(TransactionType.INITIAL_STIPEND);
            transaction.setAmount(amount);
            transaction.setBalanceBefore(balanceBefore);
            transaction.setBalanceAfter(wallet.getBalance());
            transaction.setLeagueId(leagueId);
            transaction.setUserId(userId);
            transaction.setIdempotencyKey(idempotencyKey);
            transactionRepository.save(transaction);

            stipendsIssued++;
        }

        return new StipendResultResponse(leagueId, walletsCreated, stipendsIssued, amount);
    }

    @Transactional
    public StipendResultResponse issueWeeklyStipends(Long leagueId, BigDecimal amount, List<Long> userIds, Integer weekNumber) {
        int stipendsIssued = 0;
        for (Long userId : userIds) {
            String idempotencyKey = "WEEKLY_STIPEND:" + leagueId + ":" + userId + ":" + weekNumber;

            if (transactionRepository.existsByIdempotencyKey(idempotencyKey)) {
                continue;
            }

            Wallet wallet = walletRepository.findByUserIdAndLeagueIdForUpdate(userId, leagueId).orElseThrow(
                    () -> new WalletNotFoundException("Wallet not found for user: " + userId)
            );
            BigDecimal balanceBefore = wallet.getBalance();
            wallet.setBalance(balanceBefore.add(amount));

            Transaction transaction = new Transaction();
            transaction.setWallet(wallet);
            transaction.setType(TransactionType.WEEKLY_STIPEND);
            transaction.setAmount(amount);
            transaction.setBalanceBefore(balanceBefore);
            transaction.setBalanceAfter(wallet.getBalance());
            transaction.setLeagueId(leagueId);
            transaction.setUserId(userId);
            transaction.setIdempotencyKey(idempotencyKey);
            transactionRepository.save(transaction);

            stipendsIssued++;
        }
        return new StipendResultResponse(leagueId, 0, stipendsIssued, amount);
    }

    @Transactional
    public TransactionResponse processStockBuy(Long userId, Long leagueId, BigDecimal amount, 
                                                String referenceId, String description) {
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
    public TransactionResponse processStockSell(Long userId, Long leagueId, BigDecimal amount, 
                                                 String referenceId, String description) {
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
        // TODO: Implement wallet retrieval
        // - Find wallet by userId and leagueId
        // - Throw WalletNotFoundException if not found
        // - Return WalletResponse
        throw new UnsupportedOperationException("TODO: Implement getWallet");
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(Long userId, Long leagueId, Pageable pageable) {
        // TODO: Implement transaction history retrieval
        // - Query transactions by userId and leagueId, ordered by createdAt DESC
        // - Return paginated TransactionResponse objects
        throw new UnsupportedOperationException("TODO: Implement getTransactionHistory");
    }

    @Transactional(readOnly = true)
    public List<WalletResponse> getLeagueWallets(Long leagueId) {
        // TODO: Implement league wallets retrieval
        // - Find all wallets for the given leagueId
        // - Return list of WalletResponse objects
        throw new UnsupportedOperationException("TODO: Implement getLeagueWallets");
    }
}
