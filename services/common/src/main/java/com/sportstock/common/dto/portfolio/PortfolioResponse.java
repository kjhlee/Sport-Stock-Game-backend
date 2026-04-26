package com.sportstock.common.dto.portfolio;

import java.util.List;

public record PortfolioResponse(
    Long id, Long userId, Long leagueId, List<HoldingsResponse> holdingsList) {}
