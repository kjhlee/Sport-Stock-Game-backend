package com.sportstock.ingestion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.sportstock.common.dto.ingestion.CurrentWeekResponse;
import com.sportstock.ingestion.entity.Season;
import com.sportstock.ingestion.entity.SeasonWeek;
import com.sportstock.ingestion.repo.SeasonWeekRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class SeasonQueryServiceTest {

  @Mock private SeasonWeekRepository seasonWeekRepository;

  private SeasonQueryService service;

  @BeforeEach
  void setUp() {
    service = new SeasonQueryService(seasonWeekRepository);
  }

  @Test
  void getPriorWeek_returnsPriorWeekWhenExists() {
    SeasonWeek currentWeek =
        buildSeasonWeek(
            2026,
            "2",
            "5",
            Instant.parse("2026-01-10T00:00:00Z"),
            Instant.parse("2026-01-17T00:00:00Z"));
    SeasonWeek priorWeek =
        buildSeasonWeek(
            2026,
            "2",
            "4",
            Instant.parse("2026-01-03T00:00:00Z"),
            Instant.parse("2026-01-10T00:00:00Z"));

    when(seasonWeekRepository.findCurrentWeek(any(Instant.class), eq(List.of("2", "3"))))
        .thenReturn(Optional.of(currentWeek));
    when(seasonWeekRepository.findPriorWeek(
            eq(Instant.parse("2026-01-10T00:00:00Z")), eq(List.of("2", "3"))))
        .thenReturn(Optional.of(priorWeek));

    Optional<CurrentWeekResponse> result = service.getPriorWeek();

    assertTrue(result.isPresent());
    assertEquals(2026, result.get().seasonYear());
    assertEquals("2", result.get().seasonType());
    assertEquals(4, result.get().week());
  }

  @Test
  void getPriorWeek_returnsEmptyWhenFirstWeekOfSeason() {
    SeasonWeek currentWeek =
        buildSeasonWeek(
            2026,
            "2",
            "1",
            Instant.parse("2026-09-01T00:00:00Z"),
            Instant.parse("2026-09-08T00:00:00Z"));

    when(seasonWeekRepository.findCurrentWeek(any(Instant.class), eq(List.of("2", "3"))))
        .thenReturn(Optional.of(currentWeek));
    when(seasonWeekRepository.findPriorWeek(
            eq(Instant.parse("2026-09-01T00:00:00Z")), eq(List.of("2", "3"))))
        .thenReturn(Optional.empty());

    Optional<CurrentWeekResponse> result = service.getPriorWeek();

    assertTrue(result.isEmpty());
  }

  @Test
  void getPriorWeek_crossesSeasonTypeBoundary() {
    SeasonWeek currentWeek =
        buildSeasonWeek(
            2026,
            "3",
            "1",
            Instant.parse("2026-01-15T00:00:00Z"),
            Instant.parse("2026-01-22T00:00:00Z"));
    SeasonWeek priorWeek =
        buildSeasonWeek(
            2026,
            "2",
            "18",
            Instant.parse("2026-01-08T00:00:00Z"),
            Instant.parse("2026-01-15T00:00:00Z"));

    when(seasonWeekRepository.findCurrentWeek(any(Instant.class), eq(List.of("2", "3"))))
        .thenReturn(Optional.of(currentWeek));
    when(seasonWeekRepository.findPriorWeek(
            eq(Instant.parse("2026-01-15T00:00:00Z")), eq(List.of("2", "3"))))
        .thenReturn(Optional.of(priorWeek));

    Optional<CurrentWeekResponse> result = service.getPriorWeek();

    assertTrue(result.isPresent());
    assertEquals("2", result.get().seasonType());
    assertEquals(18, result.get().week());
  }

  @Test
  void getPriorWeek_returnsEmptyWhenNoCurrentWeek() {
    when(seasonWeekRepository.findCurrentWeek(any(Instant.class), eq(List.of("2", "3"))))
        .thenReturn(Optional.empty());

    Optional<CurrentWeekResponse> result = service.getPriorWeek();

    assertTrue(result.isEmpty());
  }

  @Test
  void isSeasonActive_returnsFalseWhenNoActiveWeek() {
    when(seasonWeekRepository.findCurrentWeek(any(Instant.class), eq(List.of("2", "3"))))
        .thenReturn(Optional.empty());

    assertFalse(service.isSeasonActive());
  }

  @Test
  void isSeasonActive_rethrowsDataAccessException() {
    when(seasonWeekRepository.findCurrentWeek(any(Instant.class), eq(List.of("2", "3"))))
        .thenThrow(new DataAccessResourceFailureException("db down"));

    assertThrows(DataAccessResourceFailureException.class, () -> service.isSeasonActive());
  }

  private SeasonWeek buildSeasonWeek(
      int year, String seasonType, String weekValue, Instant startDate, Instant endDate) {
    Season season = new Season();
    season.setYear(year);
    season.setSeasonTypeName("Regular Season");

    SeasonWeek sw = new SeasonWeek();
    sw.setSeason(season);
    sw.setSeasonTypeValue(seasonType);
    sw.setWeekValue(weekValue);
    sw.setLabel("Week " + weekValue);
    sw.setStartDate(startDate);
    sw.setEndDate(endDate);
    return sw;
  }
}
