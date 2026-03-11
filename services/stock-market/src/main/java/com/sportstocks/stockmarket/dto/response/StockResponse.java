package com.sportstocks.stockmarket.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockResponse(
        UUID id,
        String athleteEspnId,
        String fullName,
        String position,
        String teamEspnId,
        BigDecimal currentPrice,
        String status,
        Instant priceUpdatedAt
) {
}