package com.sportstock.ingestion.service;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import com.sportstock.ingestion.entity.SeasonWeek;
import com.sportstock.ingestion.exception.EntityNotFoundException;
import com.sportstock.ingestion.repo.SeasonWeekRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SeasonQueryService {

  private final SeasonWeekRepository seasonWeekRepository;
  private static final List<String> SEASON_TYPES = List.of("2", "3"); // season , postseason

  @Transactional
  public CurrentWeekResponse getCurrentWeek() {
    Instant now = Instant.now();

    int currentYear = LocalDate.now(ZoneOffset.UTC).getYear();

    SeasonWeek sw =
        seasonWeekRepository
            .findCurrentWeek(now, currentYear, SEASON_TYPES)
            .or(() -> seasonWeekRepository.findCurrentWeek(now, currentYear - 1, SEASON_TYPES))
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
}
