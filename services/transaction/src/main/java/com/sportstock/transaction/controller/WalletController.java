package com.sportstock.transaction.controller;

import com.sportstock.common.dto.transaction.CreateWalletRequest;
import com.sportstock.common.dto.transaction.IssueStipendRequest;
import com.sportstock.common.dto.transaction.StipendResultResponse;
import com.sportstock.common.dto.transaction.StockTransactionRequest;
import com.sportstock.common.dto.transaction.StockTransactionResponse;
import com.sportstock.common.dto.transaction.TransactionResponse;
import com.sportstock.common.dto.transaction.WalletResponse;
import com.sportstock.common.security.CurrentUserProvider;
import com.sportstock.transaction.service.WalletService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/wallets")
public class WalletController {

  private final WalletService walletService;
  private final CurrentUserProvider currentUserProvider;

  @PostMapping("")
  @ResponseStatus(HttpStatus.CREATED)
  public WalletResponse createWallet(@Valid @RequestBody CreateWalletRequest request) {
    Long userId = currentUserProvider.getCurrentUserId();
    return walletService.createWallet(userId, request.leagueId());
  }

  @GetMapping("")
  @ResponseStatus(HttpStatus.OK)
  public WalletResponse getWallet(@RequestParam Long leagueId) {
    Long userId = currentUserProvider.getCurrentUserId();
    return walletService.getWallet(userId, leagueId);
  }

  @GetMapping("/league/{leagueId}")
  @ResponseStatus(HttpStatus.OK)
  public List<WalletResponse> getLeagueWallets(@PathVariable Long leagueId) {
    return walletService.getLeagueWallets(leagueId);
  }

  @PostMapping("/stipends/initial")
  @ResponseStatus(HttpStatus.OK)
  public StipendResultResponse issueInitialStipends(
      @Valid @RequestBody IssueStipendRequest request) {
    return walletService.issueInitialStipends(request.leagueId(), request.amount());
  }

  @PostMapping("/stipends/weekly")
  @ResponseStatus(HttpStatus.OK)
  public StipendResultResponse issueWeeklyStipends(
      @Valid @RequestBody IssueStipendRequest request, @RequestParam Integer weekNumber) {
    return walletService.issueWeeklyStipends(request.leagueId(), request.amount(), weekNumber);
  }

  @PostMapping("/buy")
  @ResponseStatus(HttpStatus.OK)
  public StockTransactionResponse buyStock(@Valid @RequestBody StockTransactionRequest request) {
    Long userId = currentUserProvider.getCurrentUserId();
    return walletService.processStockBuy(userId, request.leagueId(), request);
  }

  @PostMapping("/sell")
  @ResponseStatus(HttpStatus.OK)
  public StockTransactionResponse sellStock(@Valid @RequestBody StockTransactionRequest request) {
    Long userId = currentUserProvider.getCurrentUserId();
    return walletService.processStockSell(userId, request.leagueId(), request);
  }

  @GetMapping("/transactions")
  @ResponseStatus(HttpStatus.OK)
  public Page<TransactionResponse> getTransactionHistory(
      @RequestParam Long leagueId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Long userId = currentUserProvider.getCurrentUserId();
    return walletService.getTransactionHistory(
        userId, leagueId, PageRequest.of(page, Math.min(size, 100)));
  }
}
