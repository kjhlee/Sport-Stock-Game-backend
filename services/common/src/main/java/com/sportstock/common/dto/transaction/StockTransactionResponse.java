package com.sportstock.common.dto.transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StockTransactionResponse(
    Long transactionId,
    UUID stockId,
    String stockName,
    BigDecimal pricePerShare,
    BigDecimal quantity,
    BigDecimal totalAmount,
    BigDecimal balanceBefore,
    BigDecimal balanceAfter,
    String type,
    OffsetDateTime createdAt) {}
