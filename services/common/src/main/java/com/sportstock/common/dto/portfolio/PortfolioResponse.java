package com.sportstock.common.dto.portfolio;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class PortfolioResponse {
    private long Id;
    private Long userId;

    private Long leagueId;
    private List<HoldingsResponse> holdingsList = new ArrayList<>();

}
