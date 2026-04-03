package com.sportstock.common.dto.stock_market;

public record IngestionInjuryStatusDto(
    String athleteEspnId, String teamEspnId, String injuryStatus, String rosterStatusType) {}
