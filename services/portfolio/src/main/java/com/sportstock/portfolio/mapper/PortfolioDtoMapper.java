package com.sportstock.portfolio.mapper;

import com.sportstock.common.dto.portfolio.HoldingsResponse;
import com.sportstock.common.dto.portfolio.PortfolioResponse;
import com.sportstock.portfolio.entity.Holdings;
import com.sportstock.portfolio.entity.Portfolio;

public final class PortfolioDtoMapper {

  private PortfolioDtoMapper() {}

  public static PortfolioResponse toResponse(Portfolio portfolio) {
    return new PortfolioResponse(
        portfolio.getId(),
        portfolio.getUserId(),
        portfolio.getLeagueId(),
        portfolio.getHoldingsList().stream().map(PortfolioDtoMapper::toResponse).toList());
  }

  public static HoldingsResponse toResponse(Holdings holding) {
    return new HoldingsResponse(holding.getId(), holding.getStockId(), holding.getQuantity());
  }
}
