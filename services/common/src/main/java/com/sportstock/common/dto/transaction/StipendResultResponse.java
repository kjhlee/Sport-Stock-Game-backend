package com.sportstock.common.dto.transaction;

import java.math.BigDecimal;

public record StipendResultResponse(
    Long leagueId, int walletsCreated, int stipendsIssued, BigDecimal amountPerUser) {}
