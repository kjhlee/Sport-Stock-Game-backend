package com.sportstock.common.dto.stock_market;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


public record StockResponse(
        UUID id,
        String espnId,
        String fullName,
        String position,
        String type,
        String teamEspnId,
        BigDecimal currentPrice,
        String status,
        boolean gameLocked,
        boolean injuryLocked,
        Instant priceUpdatedAt) {}
