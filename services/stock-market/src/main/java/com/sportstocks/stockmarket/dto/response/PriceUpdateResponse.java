package com.sportstocks.stockmarket.dto.response;

public record PriceUpdateResponse(
        int updated,
        int skipped
) {
}
