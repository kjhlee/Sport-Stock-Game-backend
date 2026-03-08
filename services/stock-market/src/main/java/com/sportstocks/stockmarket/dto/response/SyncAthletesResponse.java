package com.sportstocks.stockmarket.dto.response;

public record SyncAthletesResponse(
        int created,
        int updated,
        int skipped,
        int totalFetched
) {
}