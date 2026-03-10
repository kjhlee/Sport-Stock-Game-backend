package com.sportstock.ingestion.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportstock.common.dto.ingestion.AthleteResponse;
import com.sportstock.common.dto.ingestion.BoxscoreTeamStatResponse;
import com.sportstock.common.dto.ingestion.EventResponse;
import com.sportstock.common.dto.ingestion.PlayerGameStatResponse;
import com.sportstock.common.dto.ingestion.TeamRecordResponse;
import com.sportstock.common.dto.ingestion.TeamResponse;
import com.sportstock.ingestion.entity.Athlete;
import com.sportstock.ingestion.entity.BoxscoreTeamStat;
import com.sportstock.ingestion.entity.Event;
import com.sportstock.ingestion.entity.PlayerGameStat;
import com.sportstock.ingestion.entity.Team;
import com.sportstock.ingestion.entity.TeamRecord;
import com.sportstock.ingestion.exception.IngestionException;

import java.util.Map;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static AthleteResponse toAthleteResponse(Athlete entity) {
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

    public static TeamResponse toTeamResponse(Team entity) {
        return new TeamResponse(
                entity.getEspnId(),
                entity.getAbbreviation(),
                entity.getDisplayName(),
                entity.getShortDisplayName(),
                entity.getName(),
                entity.getNickname(),
                entity.getLocation(),
                entity.getColor(),
                entity.getAlternateColor(),
                entity.getIsActive(),
                entity.getIsAllStar(),
                entity.getLogoUrl(),
                entity.getDivisionId(),
                entity.getConferenceId(),
                entity.getStandingSummary()
        );
    }

    public static EventResponse toEventResponse(Event entity) {
        return new EventResponse(
                entity.getEspnId(),
                entity.getName(),
                entity.getShortName(),
                entity.getDate(),
                entity.getSeasonYear(),
                entity.getSeasonType(),
                entity.getSeasonSlug(),
                entity.getWeekNumber(),
                entity.getAttendance(),
                entity.getNeutralSite(),
                entity.getConferenceCompetition(),
                entity.getPlayByPlayAvailable(),
                entity.getStatusState(),
                entity.getStatusCompleted(),
                entity.getStatusDescription(),
                entity.getStatusPeriod(),
                entity.getStatusClock(),
                entity.getBroadcast(),
                entity.getNoteHeadline()
        );
    }

    public static PlayerGameStatResponse toPlayerGameStatResponse(PlayerGameStat entity, ObjectMapper objectMapper) {
        Map<String, String> statsMap;
        try {
            statsMap = objectMapper.readValue(entity.getStats(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new IngestionException("Failed to parse player stats JSON", e);
        }
        return new PlayerGameStatResponse(
                entity.getEvent().getEspnId(),
                entity.getAthleteEspnId(),
                entity.getTeam().getEspnId(),
                entity.getStatCategory(),
                statsMap
        );
    }

    public static BoxscoreTeamStatResponse toBoxscoreTeamStatResponse(BoxscoreTeamStat entity) {
        return new BoxscoreTeamStatResponse(
                entity.getEvent().getEspnId(),
                entity.getTeam().getEspnId(),
                entity.getHomeAway(),
                entity.getStatName(),
                entity.getStatValue(),
                entity.getDisplayValue(),
                entity.getLabel()
        );
    }

    public static TeamRecordResponse toTeamRecordResponse(TeamRecord entity) {
        return new TeamRecordResponse(
                entity.getTeam().getEspnId(),
                entity.getSeasonYear(),
                entity.getRecordType(),
                entity.getSummary(),
                entity.getWins(),
                entity.getLosses(),
                entity.getTies(),
                entity.getWinPercent(),
                entity.getOtWins(),
                entity.getOtLosses(),
                entity.getPointsFor(),
                entity.getPointsAgainst(),
                entity.getPointDifferential(),
                entity.getAvgPointsFor(),
                entity.getAvgPointsAgainst(),
                entity.getPlayoffSeed(),
                entity.getStreak(),
                entity.getGamesPlayed(),
                entity.getDivisionWins(),
                entity.getDivisionLosses(),
                entity.getDivisionTies(),
                entity.getLeagueWinPercent()
        );
    }
}
