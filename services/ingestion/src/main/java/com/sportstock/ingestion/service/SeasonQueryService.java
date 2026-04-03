package com.sportstock.ingestion.service;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import com.sportstock.ingestion.entity.SeasonWeek;
import com.sportstock.ingestion.exception.EntityNotFoundException;
import com.sportstock.ingestion.repo.SeasonWeekRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SeasonQueryService {

  private final SeasonWeekRepository seasonWeekRepository;
  private static final List<String> SEASON_TYPES = List.of("2", "3"); // season , postseason
  private static final List<String> ACTIVE_OR_PRESEASON_TYPES = List.of("1", "2", "3");

  @Transactional
  public CurrentWeekResponse getCurrentWeek() {
    Instant now = Instant.now();
    SeasonWeek sw =
        seasonWeekRepository
            .findCurrentWeek(now, SEASON_TYPES)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "No active NFL week found for current date. Season may be in offseason or preseason"));
    return new CurrentWeekResponse(
        sw.getSeason().getYear(),
        sw.getSeasonTypeValue(),
        sw.getSeason().getSeasonTypeName(),
        Integer.parseInt(sw.getWeekValue()),
        sw.getLabel(),
        sw.getStartDate(),
        sw.getEndDate());
  }

  @Transactional
  public Optional<CurrentWeekResponse> getNextWeek() {
    Instant now = Instant.now();
    return seasonWeekRepository
        .findNextWeek(now, SEASON_TYPES)
        .map(
            sw ->
                new CurrentWeekResponse(
                    sw.getSeason().getYear(),
                    sw.getSeasonTypeValue(),
                    sw.getSeason().getSeasonTypeName(),
                    Integer.parseInt(sw.getWeekValue()),
                    sw.getLabel(),
                    sw.getStartDate(),
                    sw.getEndDate()));
  }

  public boolean isSeasonActive() {
    try {
      getCurrentWeek();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public Optional<CurrentWeekResponse> getCurrentWeekOptional() {
    try {
      return Optional.of(getCurrentWeek());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public Optional<CurrentWeekResponse> getCurrentWeekIncludingPreseasonOptional() {
    Instant now = Instant.now();
    return seasonWeekRepository
        .findCurrentWeek(now, ACTIVE_OR_PRESEASON_TYPES)
        .map(
            sw ->
                new CurrentWeekResponse(
                    sw.getSeason().getYear(),
                    sw.getSeasonTypeValue(),
                    sw.getSeason().getSeasonTypeName(),
                    Integer.parseInt(sw.getWeekValue()),
                    sw.getLabel(),
                    sw.getStartDate(),
                    sw.getEndDate()));
  }
}
