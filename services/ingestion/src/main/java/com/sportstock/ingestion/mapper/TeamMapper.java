package com.sportstock.ingestion.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportstock.ingestion.entity.Team;
import com.sportstock.ingestion.entity.TeamRecord;

import java.math.BigDecimal;
import java.time.Instant;

import static com.sportstock.ingestion.mapper.JsonNodeUtils.*;

public final class TeamMapper {

    private TeamMapper() {}

    public static void applyBaseFields(JsonNode node, Team team) {
        Instant now = Instant.now();

        team.setEspnId(node.path("id").asText());
        team.setEspnUid(textOrNull(node, "uid"));
        team.setSlug(textOrNull(node, "slug"));
        team.setAbbreviation(node.path("abbreviation").asText());
        team.setDisplayName(node.path("displayName").asText());
        team.setShortDisplayName(textOrNull(node, "shortDisplayName"));
        team.setName(textOrNull(node, "name"));
        team.setNickname(textOrNull(node, "nickname"));
        team.setLocation(textOrNull(node, "location"));
        team.setColor(textOrNull(node, "color"));
        team.setAlternateColor(textOrNull(node, "alternateColor"));
        team.setIsActive(boolOrNull(node, "isActive"));
        team.setIsAllStar(boolOrNull(node, "isAllStar"));
        team.setLogoUrl(extractFirstLogoUrl(node));

        if (team.getIngestedAt() == null) {
            team.setIngestedAt(now);
        }
        team.setUpdatedAt(now);
    }

    public static void applyDetailFields(JsonNode node, Team team) {
        JsonNode groups = node.path("groups");
        if (!groups.isMissingNode()) {
            team.setDivisionId(textOrNull(groups, "id"));
            JsonNode parent = groups.path("parent");
            if (!parent.isMissingNode()) {
                team.setConferenceId(textOrNull(parent, "id"));
            }
        }

        JsonNode franchise = node.path("franchise");
        if (!franchise.isMissingNode()) {
            team.setFranchiseId(textOrNull(franchise, "id"));
        }

        team.setStandingSummary(textOrNull(node, "standingSummary"));
    }

    public static void applyRecordFields(JsonNode item, TeamRecord record, Team team, Integer seasonYear) {
        Instant now = Instant.now();

        record.setTeam(team);
        record.setSeasonYear(seasonYear);
        record.setRecordType(truncate(item.path("type").asText(), 20));
        record.setSummary(textOrNull(item, "summary"));

        JsonNode stats = item.path("stats");
        if (stats.isArray()) {
            for (JsonNode stat : stats) {
                String name = stat.path("name").asText();
                double value = stat.path("value").asDouble(0);
                applyRecordStat(record, name, value);
            }
        }

        if (record.getIngestedAt() == null) {
            record.setIngestedAt(now);
        }
        record.setUpdatedAt(now);
    }

    private static void applyRecordStat(TeamRecord record, String name, double value) {
        switch (name) {
            case "wins" -> record.setWins((int) value);
            case "losses" -> record.setLosses((int) value);
            case "ties" -> record.setTies((int) value);
            case "winPercent" -> record.setWinPercent(BigDecimal.valueOf(value));
            case "OTWins" -> record.setOtWins((int) value);
            case "OTLosses" -> record.setOtLosses((int) value);
            case "pointsFor" -> record.setPointsFor((int) value);
            case "pointsAgainst" -> record.setPointsAgainst((int) value);
            case "pointDifferential" -> record.setPointDifferential((int) value);
            case "avgPointsFor" -> record.setAvgPointsFor(BigDecimal.valueOf(value));
            case "avgPointsAgainst" -> record.setAvgPointsAgainst(BigDecimal.valueOf(value));
            case "playoffSeed" -> record.setPlayoffSeed((int) value);
            case "streak" -> record.setStreak((int) value);
            case "gamesPlayed" -> record.setGamesPlayed((int) value);
            case "divisionWins" -> record.setDivisionWins((int) value);
            case "divisionLosses" -> record.setDivisionLosses((int) value);
            case "divisionTies" -> record.setDivisionTies((int) value);
            case "leagueWinPercent" -> record.setLeagueWinPercent(BigDecimal.valueOf(value));
            default -> { }
        }
    }
}
