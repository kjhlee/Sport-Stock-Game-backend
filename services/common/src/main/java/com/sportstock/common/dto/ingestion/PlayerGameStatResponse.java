package com.sportstock.common.dto.ingestion;

import java.util.Map;

public record PlayerGameStatResponse(
    String eventEspnId,
    String athleteEspnId,
    String teamEspnId,
    String statCategory,
    Map<String, String> stats) {}
