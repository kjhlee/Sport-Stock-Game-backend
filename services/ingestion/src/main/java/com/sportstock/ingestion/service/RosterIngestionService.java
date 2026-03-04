package com.sportstock.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportstock.ingestion.client.EspnApiClient;
import com.sportstock.ingestion.entity.Athlete;
import com.sportstock.ingestion.entity.Coach;
import com.sportstock.ingestion.entity.Team;
import com.sportstock.ingestion.entity.TeamRosterEntry;
import com.sportstock.ingestion.exception.EntityNotFoundException;
import com.sportstock.ingestion.exception.IngestionException;
import com.sportstock.ingestion.mapper.AthleteMapper;
import com.sportstock.ingestion.mapper.JsonPayloadCodec;
import com.sportstock.ingestion.repo.AthleteRepository;
import com.sportstock.ingestion.repo.CoachRepository;
import com.sportstock.ingestion.repo.TeamRepository;
import com.sportstock.ingestion.repo.TeamRosterEntryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RosterIngestionService {

    private final EspnApiClient espnApiClient;
    private final TeamRepository teamRepository;
    private final AthleteRepository athleteRepository;
    private final TeamRosterEntryRepository teamRosterEntryRepository;
    private final CoachRepository coachRepository;
    private final JsonPayloadCodec jsonPayloadCodec;
    private final TransactionTemplate transactionTemplate;

    public RosterIngestionService(
            EspnApiClient espnApiClient,
            TeamRepository teamRepository,
            AthleteRepository athleteRepository,
            TeamRosterEntryRepository teamRosterEntryRepository,
            CoachRepository coachRepository,
            JsonPayloadCodec jsonPayloadCodec,
            TransactionTemplate transactionTemplate
    ) {
        this.espnApiClient = espnApiClient;
        this.teamRepository = teamRepository;
        this.athleteRepository = athleteRepository;
        this.teamRosterEntryRepository = teamRosterEntryRepository;
        this.coachRepository = coachRepository;
        this.jsonPayloadCodec = jsonPayloadCodec;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional
    public void ingestTeamRoster(String teamEspnId, Integer seasonYear, Integer rosterLimit) {
        ingestTeamRosterInternal(teamEspnId, seasonYear, rosterLimit);
    }

    private void ingestTeamRosterInternal(String teamEspnId, Integer seasonYear, Integer rosterLimit) {
        Team team = teamRepository.findByEspnId(teamEspnId)
                .orElseThrow(() -> new EntityNotFoundException("Team not found with ESPN ID: " + teamEspnId));

        String json = espnApiClient.fetchTeamRoster(teamEspnId);
        JsonNode root = jsonPayloadCodec.parseJson(json);
        JsonNode athleteGroups = root.path("athletes");

        if (!athleteGroups.isArray()) {
            throw new IngestionException("Unexpected ESPN roster response for team " + teamEspnId);
        }

        int count = 0;
        for (JsonNode group : athleteGroups) {
            String rosterGroup = group.path("position").asText("unknown");
            JsonNode items = group.path("items");
            if (!items.isArray()) {
                continue;
            }

            for (JsonNode athleteNode : items) {
                if (rosterLimit != null && count >= rosterLimit) {
                    break;
                }

                Athlete athlete = upsertAthlete(athleteNode);

                TeamRosterEntry entry = teamRosterEntryRepository
                        .findByTeamIdAndAthleteIdAndSeasonYear(team.getId(), athlete.getId(), seasonYear)
                        .orElseGet(TeamRosterEntry::new);
                AthleteMapper.applyRosterEntryFields(athleteNode, entry, team, athlete, seasonYear, rosterGroup);
                teamRosterEntryRepository.save(entry);
                count++;
            }
            if (rosterLimit != null && count >= rosterLimit) {
                break;
            }
        }
        JsonNode coachArray = root.path("coach");
        int seasonYearValue = root.path("season").path("year").asInt(0);
        if (seasonYearValue == 0) {
            seasonYearValue = seasonYear;
        }
        if (coachArray.isArray()) {
            for (JsonNode coachNode : coachArray) {
                String coachEspnId = coachNode.path("id").asText();
                Coach coach = coachRepository
                        .findByEspnIdAndTeamIdAndSeasonYear(coachEspnId, team.getId(), seasonYearValue)
                        .orElseGet(Coach::new);
                AthleteMapper.applyCoachFields(coachNode, coach, team, seasonYearValue);
                coachRepository.save(coach);
            }
        }

        log.info("Ingested {} roster entries for team {} ({})", count, team.getDisplayName(), teamEspnId);
    }

    private Athlete upsertAthlete(JsonNode athleteNode) {
        String espnId = athleteNode.path("id").asText();
        Athlete athlete = athleteRepository.findByEspnId(espnId).orElseGet(Athlete::new);
        AthleteMapper.applyFields(athleteNode, athlete);

        try {
            return athleteRepository.save(athlete);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Athlete {} was inserted concurrently during roster sync; reloading existing row", espnId);
            Athlete existing = athleteRepository.findByEspnId(espnId)
                    .orElseThrow(() -> ex);
            AthleteMapper.applyFields(athleteNode, existing);
            return athleteRepository.save(existing);
        }
    }

    public void ingestAllRosters(Integer seasonYear, Integer rosterLimit, List<String> teamEspnIds) {
        List<Team> teams;
        if (teamEspnIds != null && !teamEspnIds.isEmpty()) {
            teams = teamEspnIds.stream()
                    .map(id -> teamRepository.findByEspnId(id)
                            .orElseThrow(() -> new EntityNotFoundException("Team not found with ESPN ID: " + id)))
                    .toList();
        } else {
            teams = teamRepository.findAllByOrderByDisplayNameAsc();
        }

        int success = 0;
        int failed = 0;
        List<String> failedIds = new ArrayList<>();

        for (Team team : teams) {
            try {
                transactionTemplate.executeWithoutResult(status ->
                        ingestTeamRosterInternal(team.getEspnId(), seasonYear, rosterLimit));
                success++;
            } catch (Exception e) {
                failed++;
                failedIds.add(team.getEspnId());
                log.error("Failed to ingest roster for team {}: {}", team.getEspnId(), e.getMessage());
            }
        }
        log.info("Ingested rosters for {} teams ({} failed{})",
                success, failed, failedIds.isEmpty() ? "" : ", IDs: " + failedIds);
    }
}
