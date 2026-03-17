package com.sportstock.common.dto.stock_market;

import java.util.List;

public record PagedStockResponse(
    List<StockResponse> content, int page, int size, long totalElements, int totalPages) {}
