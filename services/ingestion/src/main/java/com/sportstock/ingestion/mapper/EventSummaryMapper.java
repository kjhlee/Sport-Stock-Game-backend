package com.sportstock.ingestion.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportstock.ingestion.entity.BoxscoreTeamStat;
import com.sportstock.ingestion.entity.Event;
import com.sportstock.ingestion.entity.Team;

import static com.sportstock.ingestion.mapper.JsonNodeUtils.*;

public final class EventSummaryMapper {

    private EventSummaryMapper() {}

    public static void applyBoxscoreStatFields(JsonNode statNode, BoxscoreTeamStat stat, Event event, Team team, String homeAway) {
        stat.setEvent(event);
        stat.setTeam(team);
        stat.setHomeAway(homeAway);
        stat.setStatName(statNode.path("name").asText());
        stat.setDisplayValue(textOrNull(statNode, "displayValue"));
        stat.setLabel(textOrNull(statNode, "label"));

        JsonNode valueNode = statNode.path("value");
        if (!valueNode.isMissingNode() && !valueNode.isNull()) {
            stat.setStatValue(valueNode.asText());
        }
    }
}
