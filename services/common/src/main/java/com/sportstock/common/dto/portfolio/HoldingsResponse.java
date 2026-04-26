package com.sportstock.common.dto.portfolio;

import java.math.BigDecimal;
import java.util.UUID;

public record HoldingsResponse(Long id, UUID stockId, BigDecimal quantity) {}
