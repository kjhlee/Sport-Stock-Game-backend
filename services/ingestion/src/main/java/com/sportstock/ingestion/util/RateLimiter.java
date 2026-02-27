package com.sportstock.ingestion.util;

import com.sportstock.ingestion.config.EspnApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimiter {

    private final EspnApiProperties espnApiProperties;
    private long nextAllowedAtMs = 0L;

    public synchronized void acquirePermit() {
        int delayMs = espnApiProperties.getRateLimitDelayMs();
        if (delayMs <= 0) {
            return;
        }

        long now = System.currentTimeMillis();
        long waitMs = nextAllowedAtMs - now;
        if (waitMs > 0) {
            sleep(waitMs);
            now = System.currentTimeMillis();
        }
        nextAllowedAtMs = now + delayMs;
    }

    private void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for ESPN rate limit permit", e);
        }
    }
}
