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

    public void pause() {
        int delayMs = espnApiProperties.getRateLimitDelayMs();
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
