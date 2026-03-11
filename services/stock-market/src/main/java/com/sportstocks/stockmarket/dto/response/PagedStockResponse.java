package com.sportstocks.stockmarket.dto.response;

import java.util.List;

public record PagedStockResponse(
        List<StockResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}