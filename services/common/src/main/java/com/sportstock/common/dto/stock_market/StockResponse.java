package com.sportstock.common.dto.stock_market;

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
    Instant priceUpdatedAt) {}
