package com.sportstock.common.dto.ingestion;

import java.math.BigDecimal;
import java.time.Instant;

public record AthleteResponse(
    String espnId,
    String firstName,
    String lastName,
    String fullName,
    String displayName,
    String shortName,
    BigDecimal weight,
    BigDecimal height,
    Integer age,
    Instant dateOfBirth,
    Integer debutYear,
    String jersey,
    String positionId,
    String positionName,
    String positionAbbreviation,
    String positionParentName,
    String positionParentAbbreviation,
    String birthCity,
    String birthState,
    String birthCountry,
    String collegeEspnId,
    String collegeName,
    String collegeAbbreviation,
    String headshotUrl,
    Integer experienceYears,
    String statusId,
    String statusName,
    String statusType,
    String handType,
    String teamEspnId) {}
