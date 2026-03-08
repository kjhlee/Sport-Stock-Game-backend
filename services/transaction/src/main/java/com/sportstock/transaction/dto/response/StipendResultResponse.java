package com.sportstock.transaction.dto.response;

import java.math.BigDecimal;

public record StipendResultResponse(
        Long leagueId,
        int walletsCreated,
        int stipendsIssued,
        BigDecimal amountPerUser
) {
}
