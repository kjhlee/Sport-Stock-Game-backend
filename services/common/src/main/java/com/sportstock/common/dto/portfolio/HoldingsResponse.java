package com.sportstock.common.dto.portfolio;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class HoldingsResponse {
    private Long id;
    private UUID stockId;
    private int quantity;
    private BigDecimal avgCostBasis;
}
