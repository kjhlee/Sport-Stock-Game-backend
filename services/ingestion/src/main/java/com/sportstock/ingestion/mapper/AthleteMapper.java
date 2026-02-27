package com.sportstock.ingestion.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportstock.ingestion.entity.Athlete;
import com.sportstock.ingestion.entity.TeamRosterEntry;
import com.sportstock.ingestion.entity.Team;

import java.math.BigDecimal;
import java.time.Instant;

import static com.sportstock.ingestion.mapper.JsonNodeUtils.*;

public final class AthleteMapper {

    private AthleteMapper() {}

    public static void applyFields(JsonNode node, Athlete athlete) {
        Instant now = Instant.now();

        athlete.setEspnId(node.path("id").asText());
        athlete.setEspnUid(textOrNull(node, "uid"));
        athlete.setEspnGuid(textOrNull(node, "guid"));
        athlete.setFirstName(textOrNull(node, "firstName"));
        athlete.setLastName(textOrNull(node, "lastName"));
        athlete.setFullName(node.path("fullName").asText());
        athlete.setDisplayName(textOrNull(node, "displayName"));
        athlete.setShortName(textOrNull(node, "shortName"));
        athlete.setSlug(textOrNull(node, "slug"));

        Double weight = doubleOrNull(node, "weight");
        if (weight != null) {
            athlete.setWeight(BigDecimal.valueOf(weight));
        }
        Double height = doubleOrNull(node, "height");
        if (height != null) {
            athlete.setHeight(BigDecimal.valueOf(height));
        }

        athlete.setAge(intOrNull(node, "age"));
        athlete.setDebutYear(intOrNull(node, "debutYear"));
        athlete.setJersey(textOrNull(node, "jersey"));

        String dob = textOrNull(node, "dateOfBirth");
        if (dob != null) {
            athlete.setDateOfBirth(parseInstantOrNull(dob));
        }

        JsonNode position = node.path("position");
        if (!position.isMissingNode()) {
            athlete.setPositionId(textOrNull(position, "id"));
            athlete.setPositionName(textOrNull(position, "name"));
            athlete.setPositionAbbreviation(textOrNull(position, "abbreviation"));
            JsonNode parent = position.path("parent");
            if (!parent.isMissingNode()) {
                athlete.setPositionParentName(textOrNull(parent, "name"));
                athlete.setPositionParentAbbreviation(textOrNull(parent, "abbreviation"));
            }
        }

        JsonNode birthPlace = node.path("birthPlace");
        if (!birthPlace.isMissingNode()) {
            athlete.setBirthCity(textOrNull(birthPlace, "city"));
            athlete.setBirthState(textOrNull(birthPlace, "state"));
            athlete.setBirthCountry(textOrNull(birthPlace, "country"));
        }

        JsonNode college = node.path("college");
        if (!college.isMissingNode()) {
            athlete.setCollegeEspnId(textOrNull(college, "id"));
            athlete.setCollegeName(textOrNull(college, "name"));
            athlete.setCollegeAbbreviation(textOrNull(college, "abbrev"));
        }

        JsonNode headshot = node.path("headshot");
        if (!headshot.isMissingNode()) {
            athlete.setHeadshotUrl(textOrNull(headshot, "href"));
        }

        JsonNode experience = node.path("experience");
        if (!experience.isMissingNode()) {
            athlete.setExperienceYears(intOrNull(experience, "years"));
        }

        JsonNode status = node.path("status");
        if (!status.isMissingNode()) {
            athlete.setStatusId(textOrNull(status, "id"));
            athlete.setStatusName(textOrNull(status, "name"));
            athlete.setStatusType(textOrNull(status, "type"));
        }

        JsonNode altIds = node.path("alternateIds");
        if (!altIds.isMissingNode()) {
            athlete.setAlternateIdsSdr(textOrNull(altIds, "sdr"));
        }

        if (athlete.getIngestedAt() == null) {
            athlete.setIngestedAt(now);
        }
        athlete.setUpdatedAt(now);
    }

    public static void applyRosterEntryFields(JsonNode node, TeamRosterEntry entry, Team team, Athlete athlete, Integer seasonYear, String rosterGroup) {
        Instant now = Instant.now();

        entry.setTeam(team);
        entry.setAthlete(athlete);
        entry.setSeasonYear(seasonYear);
        entry.setRosterGroup(rosterGroup);
        entry.setJersey(textOrNull(node, "jersey"));

        JsonNode position = node.path("position");
        if (!position.isMissingNode()) {
            entry.setPositionAbbreviation(textOrNull(position, "abbreviation"));
        }

        JsonNode status = node.path("status");
        if (!status.isMissingNode()) {
            entry.setStatusType(textOrNull(status, "type"));
        }

        JsonNode injuries = node.path("injuries");
        if (injuries.isArray() && !injuries.isEmpty()) {
            JsonNode injury = injuries.get(0);
            entry.setInjuryStatus(textOrNull(injury, "status"));
            String injuryDate = textOrNull(injury, "date");
            if (injuryDate != null) {
                entry.setInjuryDate(parseInstantOrNull(injuryDate));
            }
        }

        if (entry.getIngestedAt() == null) {
            entry.setIngestedAt(now);
        }
        entry.setUpdatedAt(now);
    }
}
