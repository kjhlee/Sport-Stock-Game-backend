package com.sportstock.transaction.service;

import com.sportstock.transaction.dto.response.StipendResultResponse;
import com.sportstock.transaction.dto.response.TransactionResponse;
import com.sportstock.transaction.dto.response.WalletResponse;
import com.sportstock.transaction.entity.Wallet;
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
        // TODO: Implement wallet creation
        // - Check if wallet already exists, throw WalletAlreadyExistsException if so
        // - Create new Wallet entity with balance 0
        // - Save and return WalletResponse
        throw new UnsupportedOperationException("TODO: Implement createWallet");
    }

    @Transactional
    public StipendResultResponse issueInitialStipends(Long leagueId, BigDecimal amount, List<Long> userIds) {
        // TODO: Implement initial stipend issuance
        // - For each user:
        //   - Create wallet if it doesn't exist
        //   - Generate idempotency key: "INITIAL_STIPEND:{leagueId}:{userId}"
        //   - Check if stipend already issued (existsByIdempotencyKey)
        //   - If not, lock wallet (findByUserIdAndLeagueIdForUpdate)
        //   - Credit balance
        //   - Create Transaction record with type INITIAL_STIPEND
        // - Return StipendResultResponse with counts
        throw new UnsupportedOperationException("TODO: Implement issueInitialStipends");
    }

    @Transactional
    public StipendResultResponse issueWeeklyStipends(Long leagueId, BigDecimal amount, List<Long> userIds, Integer weekNumber) {
        // TODO: Implement weekly stipend issuance
        // - For each user:
        //   - Generate idempotency key: "WEEKLY_STIPEND:{leagueId}:{userId}:{weekNumber}"
        //   - Check if stipend already issued (existsByIdempotencyKey)
        //   - If not, lock wallet (findByUserIdAndLeagueIdForUpdate)
        //   - Credit balance
        //   - Create Transaction record with type WEEKLY_STIPEND
        // - Return StipendResultResponse with counts
        throw new UnsupportedOperationException("TODO: Implement issueWeeklyStipends");
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
