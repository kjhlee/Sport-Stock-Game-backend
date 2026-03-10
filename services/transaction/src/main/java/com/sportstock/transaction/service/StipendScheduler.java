package com.sportstock.transaction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class StipendScheduler {

    private final WalletService walletService;

    @Scheduled(cron = "0 0 0 * * *")
    public void processWeeklyStipends() {
        // TODO: Implement weekly stipend scheduler
        // - Get current day of week (0=Sunday, 6=Saturday)
        // - Call league service internal endpoint to get active leagues matching this payout day
        //   GET /api/v1/leagues/internal/active-for-payout?dayOfWeek={0-6}
        // - For each league returned:
        //   - Extract leagueId, stipendAmount, member userIds, current week number
        //   - Call walletService.issueWeeklyStipends(leagueId, amount, userIds, weekNumber)
        // - Log results for each league
        log.info("Weekly stipend scheduler triggered for {}", LocalDate.now().getDayOfWeek());
        throw new UnsupportedOperationException("TODO: Implement processWeeklyStipends");
    }

    private int getDayOfWeekValue(DayOfWeek dayOfWeek) {
        // TODO: Helper to convert DayOfWeek to 0-6 (Sunday=0)
        // Java DayOfWeek: MONDAY=1, SUNDAY=7
        // Need to convert to: SUNDAY=0, MONDAY=1, ..., SATURDAY=6
        throw new UnsupportedOperationException("TODO: Implement getDayOfWeekValue");
    }
}
