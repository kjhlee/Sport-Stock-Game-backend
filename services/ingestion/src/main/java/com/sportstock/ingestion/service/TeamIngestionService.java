package com.sportstock.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportstock.ingestion.client.EspnApiClient;
import com.sportstock.ingestion.entity.Team;
import com.sportstock.ingestion.entity.TeamRecord;
import com.sportstock.ingestion.exception.EntityNotFoundException;
import com.sportstock.ingestion.exception.IngestionException;
import com.sportstock.ingestion.mapper.JsonNodeUtils;
import com.sportstock.ingestion.mapper.TeamMapper;
import com.sportstock.ingestion.repo.TeamRecordRepository;
import com.sportstock.ingestion.repo.TeamRepository;
import com.sportstock.ingestion.util.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamIngestionService {

    private final EspnApiClient espnApiClient;
    private final TeamRepository teamRepository;
    private final TeamRecordRepository teamRecordRepository;
    private final RateLimiter rateLimiter;

    @Transactional
    public void ingestTeams() {
        String json = espnApiClient.fetchTeams();
        JsonNode root = JsonNodeUtils.parseJson(json);

        JsonNode teamsArray = root.path("sports").path(0)
                .path("leagues").path(0)
                .path("teams");

        if (!teamsArray.isArray()) {
            throw new IngestionException("Unexpected ESPN teams response structure");
        }

        int count = 0;
        for (JsonNode wrapper : teamsArray) {
            JsonNode teamNode = wrapper.path("team");
            if (teamNode.isMissingNode()) {
                continue;
            }
            upsertTeamFromNode(teamNode);
            count++;
        }
        log.info("Ingested {} teams from ESPN teams list", count);
    }

    @Transactional
    public void ingestTeamDetail(String teamEspnId) {
        String json = espnApiClient.fetchTeamDetail(teamEspnId);
        JsonNode root = JsonNodeUtils.parseJson(json);
        JsonNode teamNode = root.path("team");

        if (teamNode.isMissingNode()) {
            throw new IngestionException("Unexpected ESPN team detail response for team " + teamEspnId);
        }

        Team team = upsertTeamFromNode(teamNode);
        TeamMapper.applyDetailFields(teamNode, team);
        teamRepository.save(team);

        upsertRecords(teamNode, team);
        log.info("Ingested detail for team {} ({})", team.getDisplayName(), teamEspnId);
    }

    @Transactional
    public void ingestAllTeamDetails() {
        List<Team> teams = teamRepository.findAllByOrderByDisplayNameAsc();
        for (Team team : teams) {
            ingestTeamDetail(team.getEspnId());
            rateLimiter.pause();
        }
        log.info("Ingested details for {} teams", teams.size());
    }

    public List<Team> listTeams() {
        return teamRepository.findAllByOrderByDisplayNameAsc();
    }

    public Team getTeamByEspnId(String teamEspnId) {
        return teamRepository.findByEspnId(teamEspnId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found with ESPN ID: " + teamEspnId));
    }

    private Team upsertTeamFromNode(JsonNode teamNode) {
        String espnId = teamNode.path("id").asText();
        Team team = teamRepository.findByEspnId(espnId).orElseGet(Team::new);
        TeamMapper.applyBaseFields(teamNode, team);
        return teamRepository.save(team);
    }

    private void upsertRecords(JsonNode teamNode, Team team) {
        JsonNode items = teamNode.path("record").path("items");
        if (!items.isArray()) {
            return;
        }

        int seasonYear = LocalDate.now().getYear();
        for (JsonNode item : items) {
            String recordType = item.path("type").asText();
            TeamRecord record = teamRecordRepository
                    .findByTeamIdAndSeasonYearAndRecordType(team.getId(), seasonYear, recordType)
                    .orElseGet(TeamRecord::new);
            TeamMapper.applyRecordFields(item, record, team, seasonYear);
            teamRecordRepository.save(record);
        }
    }

}
