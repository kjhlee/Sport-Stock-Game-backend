package com.sportstock.common.dto.stock_market;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceHistoryResponse(
    int seasonYear, int seasonType, int week, BigDecimal price, Instant recordedAt) {}
