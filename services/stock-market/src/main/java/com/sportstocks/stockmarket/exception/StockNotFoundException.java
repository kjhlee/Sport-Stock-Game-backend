package com.sportstocks.stockmarket.exception;

import java.util.UUID;

public class StockNotFoundException extends RuntimeException {

    public StockNotFoundException(UUID stockId) {
        super("Stock not found with ID: " + stockId);
    }
}