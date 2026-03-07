package com.sportstock.ingestion.dto.response;

import com.sportstock.ingestion.entity.Athlete;

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
        String handType
) {
    public static AthleteResponse from(Athlete entity) {
        return new AthleteResponse(
                entity.getEspnId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getFullName(),
                entity.getDisplayName(),
                entity.getShortName(),
                entity.getWeight(),
                entity.getHeight(),
                entity.getAge(),
                entity.getDateOfBirth(),
                entity.getDebutYear(),
                entity.getJersey(),
                entity.getPositionId(),
                entity.getPositionName(),
                entity.getPositionAbbreviation(),
                entity.getPositionParentName(),
                entity.getPositionParentAbbreviation(),
                entity.getBirthCity(),
                entity.getBirthState(),
                entity.getBirthCountry(),
                entity.getCollegeEspnId(),
                entity.getCollegeName(),
                entity.getCollegeAbbreviation(),
                entity.getHeadshotUrl(),
                entity.getExperienceYears(),
                entity.getStatusId(),
                entity.getStatusName(),
                entity.getStatusType(),
                entity.getHandType()
        );
    }
}
