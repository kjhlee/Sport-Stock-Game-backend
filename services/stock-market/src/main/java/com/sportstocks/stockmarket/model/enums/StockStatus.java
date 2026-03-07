package com.sportstocks.stockmarket.model.enums;

public enum StockStatus {
    ACTIVE, // The stock is tradeable and available for buying / selling.
    SUSPENDED, // Trading of the stock has been temporarily halted (e.g., injury or bye week).
    DELISTED // The stock has been removed from the exchange and is no longer available for trading.
}
