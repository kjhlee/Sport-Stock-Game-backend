package com.sportstock.ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sportstock.common.dto.ingestion.TeamRecordResponse;
import com.sportstock.common.dto.ingestion.TeamResponse;
import com.sportstock.ingestion.client.EspnApiClient;
import com.sportstock.ingestion.entity.Team;
import com.sportstock.ingestion.entity.TeamRecord;
import com.sportstock.ingestion.exception.EntityNotFoundException;
import com.sportstock.ingestion.exception.IngestionException;
import com.sportstock.ingestion.mapper.DtoMapper;
import com.sportstock.ingestion.mapper.JsonPayloadCodec;
import com.sportstock.ingestion.mapper.TeamMapper;
import com.sportstock.ingestion.repo.TeamRecordRepository;
import com.sportstock.ingestion.repo.TeamRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class TeamIngestionService {

  private final EspnApiClient espnApiClient;
  private final TeamRepository teamRepository;
  private final TeamRecordRepository teamRecordRepository;
  private final JsonPayloadCodec jsonPayloadCodec;
  private final TransactionTemplate transactionTemplate;

  public TeamIngestionService(
      EspnApiClient espnApiClient,
      TeamRepository teamRepository,
      TeamRecordRepository teamRecordRepository,
      JsonPayloadCodec jsonPayloadCodec,
      TransactionTemplate transactionTemplate) {
    this.espnApiClient = espnApiClient;
    this.teamRepository = teamRepository;
    this.teamRecordRepository = teamRecordRepository;
    this.jsonPayloadCodec = jsonPayloadCodec;
    this.transactionTemplate = transactionTemplate;
  }

  @Transactional
  public void ingestTeams() {
    String json = espnApiClient.fetchTeams();
    JsonNode root = jsonPayloadCodec.parseJson(json);

    JsonNode teamsArray = root.path("sports").path(0).path("leagues").path(0).path("teams");

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
  public void ingestTeamDetail(String teamEspnId, Integer seasonYear) {
    ingestTeamDetailInternal(teamEspnId, seasonYear);
  }

  private void ingestTeamDetailInternal(String teamEspnId, Integer seasonYear) {
    String json = espnApiClient.fetchTeamDetail(teamEspnId);
    JsonNode root = jsonPayloadCodec.parseJson(json);
    JsonNode teamNode = root.path("team");

    if (teamNode.isMissingNode()) {
      throw new IngestionException("Unexpected ESPN team detail response for team " + teamEspnId);
    }

    Team team = upsertTeamFromNode(teamNode);
    TeamMapper.applyDetailFields(teamNode, team);
    teamRepository.save(team);

    upsertRecords(teamNode, team, seasonYear);
    log.info("Ingested detail for team {} ({})", team.getDisplayName(), teamEspnId);
  }

  public void ingestAllTeamDetails(Integer seasonYear) {
    List<Team> teams = teamRepository.findAllByOrderByDisplayNameAsc();
    int success = 0;
    int failed = 0;
    List<String> failedIds = new ArrayList<>();

    for (Team team : teams) {
      try {
        transactionTemplate.executeWithoutResult(
            status -> ingestTeamDetailInternal(team.getEspnId(), seasonYear));
        success++;
      } catch (Exception e) {
        failed++;
        failedIds.add(team.getEspnId());
        log.error("Failed to ingest detail for team {}: {}", team.getEspnId(), e.getMessage());
      }
    }
    log.info(
        "Ingested {} team details ({} failed{})",
        success,
        failed,
        failedIds.isEmpty() ? "" : ", IDs: " + failedIds);
  }

  public List<TeamResponse> listTeams() {
    return teamRepository.findAllByOrderByDisplayNameAsc().stream()
        .map(DtoMapper::toTeamResponse)
        .toList();
  }

  public TeamResponse getTeamByEspnId(String teamEspnId) {
    return DtoMapper.toTeamResponse(findTeamEntity(teamEspnId));
  }

  @Transactional(readOnly = true)
  public List<TeamRecordResponse> listRecordsByTeam(String teamEspnId, Integer seasonYear) {
    Team team = findTeamEntity(teamEspnId);
    return teamRecordRepository.findByTeamIdAndSeasonYear(team.getId(), seasonYear).stream()
        .map(DtoMapper::toTeamRecordResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public TeamRecordResponse getRecord(String teamEspnId, Integer seasonYear, String recordType) {
    Team team = findTeamEntity(teamEspnId);
    return teamRecordRepository
        .findByTeamIdAndSeasonYearAndRecordType(team.getId(), seasonYear, recordType)
        .map(DtoMapper::toTeamRecordResponse)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "Record not found for team "
                        + teamEspnId
                        + " season "
                        + seasonYear
                        + " type "
                        + recordType));
  }

  private Team findTeamEntity(String teamEspnId) {
    return teamRepository
        .findByEspnId(teamEspnId)
        .orElseThrow(
            () -> new EntityNotFoundException("Team not found with ESPN ID: " + teamEspnId));
  }

  private Team upsertTeamFromNode(JsonNode teamNode) {
    String espnId = teamNode.path("id").asText();
    Team team = teamRepository.findByEspnId(espnId).orElseGet(Team::new);
    TeamMapper.applyBaseFields(teamNode, team);
    return teamRepository.save(team);
  }

  private void upsertRecords(JsonNode teamNode, Team team, Integer seasonYear) {
    JsonNode items = teamNode.path("record").path("items");
    if (!items.isArray()) {
      return;
    }

    for (JsonNode item : items) {
      String recordType = item.path("type").asText();
      TeamRecord record =
          teamRecordRepository
              .findByTeamIdAndSeasonYearAndRecordType(team.getId(), seasonYear, recordType)
              .orElseGet(TeamRecord::new);
      TeamMapper.applyRecordFields(item, record, team, seasonYear);
      teamRecordRepository.save(record);
    }
  }
}
