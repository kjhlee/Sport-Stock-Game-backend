package com.sportstock.ingestion.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportstock.ingestion.entity.Event;
import com.sportstock.ingestion.entity.PlayerGameStat;
import com.sportstock.ingestion.repo.PlayerGameStatRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests use real ESPN event summary boxscore key/value format as returned by the ESPN API at
 * site.api.espn.com/apis/site/v2/sports/football/nfl/summary. The keys array from each stat
 * category is zipped with each athlete's stats array to produce the JSON stored in
 * player_game_stats.stats.
 *
 * <p>Source game: KC vs ATL, 2024 Week 3 (event 401671793) and KC vs BAL, 2024 Week 1.
 */
class PlayerGameStatsFantasyPointCalculatorTest {

  private PlayerGameStatRepository repository;
  private PlayerGameStatsFantasyPointCalculator calculator;

  @BeforeEach
  void setUp() {
    repository = Mockito.mock(PlayerGameStatRepository.class);
    calculator = new PlayerGameStatsFantasyPointCalculator(repository, new ObjectMapper());
  }

  // ── QB: Patrick Mahomes (KC vs ATL, Week 3) ──────────────────────────────
  // passing: 26/39, 217 yds, 2 TD, 1 INT
  // rushing: 4 att, 21 yds, 0 TD
  // receiving: none
  // PPR = 217*0.04 + 2*4 + 1*(-2) + 21*0.1 + 0*6 = 8.68 + 8 - 2 + 2.1 = 16.78
  @Test
  void qbMahomesWeek3() {
    when(repository.findByEventIdAndAthleteEspnId(1L, "3139477"))
        .thenReturn(
            List.of(
                statRow(
                    1L,
                    "3139477",
                    "passing",
                    "{\"completions/passingAttempts\":\"26/39\",\"passingYards\":\"217\",\"yardsPerPassAttempt\":\"5.6\",\"passingTouchdowns\":\"2\",\"interceptions\":\"1\",\"sacks-sackYardsLost\":\"0-0\",\"adjQBR\":\"55.8\",\"QBRating\":\"87.2\"}"),
                statRow(
                    1L,
                    "3139477",
                    "rushing",
                    "{\"rushingAttempts\":\"4\",\"rushingYards\":\"21\",\"yardsPerRushAttempt\":\"5.3\",\"rushingTouchdowns\":\"0\",\"longRushing\":\"9\"}")));

    assertEquals(new BigDecimal("16.78"), calculator.computePlayerFantasyPoints(1L, "3139477"));
  }

  // ── RB: Isiah Pacheco (KC vs BAL, Week 1) ────────────────────────────────
  // rushing: 15 att, 45 yds, 1 TD
  // receiving: 2 rec, 33 yds, 0 TD
  // PPR = 45*0.1 + 1*6 + 2*1 + 33*0.1 = 4.5 + 6 + 2 + 3.3 = 15.80
  @Test
  void rbPachecoWeek1() {
    when(repository.findByEventIdAndAthleteEspnId(2L, "4241457"))
        .thenReturn(
            List.of(
                statRow(
                    2L,
                    "4241457",
                    "rushing",
                    "{\"rushingAttempts\":\"15\",\"rushingYards\":\"45\",\"yardsPerRushAttempt\":\"3.0\",\"rushingTouchdowns\":\"1\",\"longRushing\":\"9\"}"),
                statRow(
                    2L,
                    "4241457",
                    "receiving",
                    "{\"receptions\":\"2\",\"receivingYards\":\"33\",\"yardsPerReception\":\"16.5\",\"receivingTouchdowns\":\"0\",\"longReception\":\"40\",\"receivingTargets\":\"3\"}")));

    assertEquals(new BigDecimal("15.80"), calculator.computePlayerFantasyPoints(2L, "4241457"));
  }

  // ── WR: Rashee Rice (KC vs ATL, Week 3) ───────────────────────────────────
  // receiving: 12 rec, 110 yds, 1 TD
  // PPR = 12*1 + 110*0.1 + 1*6 = 12 + 11 + 6 = 29.00
  @Test
  void wrRiceWeek3() {
    when(repository.findByEventIdAndAthleteEspnId(1L, "4426388"))
        .thenReturn(
            List.of(
                statRow(
                    1L,
                    "4426388",
                    "receiving",
                    "{\"receptions\":\"12\",\"receivingYards\":\"110\",\"yardsPerReception\":\"9.2\",\"receivingTouchdowns\":\"1\",\"longReception\":\"27\",\"receivingTargets\":\"14\"}")));

    assertEquals(new BigDecimal("29.00"), calculator.computePlayerFantasyPoints(1L, "4426388"));
  }

  // ── TE: Travis Kelce (KC vs BAL, Week 1) ──────────────────────────────────
  // receiving: 3 rec, 34 yds, 0 TD
  // PPR = 3*1 + 34*0.1 = 3 + 3.4 = 6.40
  @Test
  void teKelceWeek1() {
    when(repository.findByEventIdAndAthleteEspnId(2L, "2519036"))
        .thenReturn(
            List.of(
                statRow(
                    2L,
                    "2519036",
                    "receiving",
                    "{\"receptions\":\"3\",\"receivingYards\":\"34\",\"yardsPerReception\":\"11.3\",\"receivingTouchdowns\":\"0\",\"longReception\":\"10\",\"receivingTargets\":\"4\"}")));

    assertEquals(new BigDecimal("6.40"), calculator.computePlayerFantasyPoints(2L, "2519036"));
  }

  // ── WR with rushing: Rashee Rice (KC vs BAL, Week 1) ──────────────────────
  // receiving: 7 rec, 103 yds, 0 TD
  // rushing: (not listed in this game summary, but include a zero-yard entry)
  // PPR = 7*1 + 103*0.1 = 7 + 10.3 = 17.30
  @Test
  void wrRiceWeek1ReceivingOnly() {
    when(repository.findByEventIdAndAthleteEspnId(2L, "4426388"))
        .thenReturn(
            List.of(
                statRow(
                    2L,
                    "4426388",
                    "receiving",
                    "{\"receptions\":\"7\",\"receivingYards\":\"103\",\"yardsPerReception\":\"14.7\",\"receivingTouchdowns\":\"0\",\"longReception\":\"73\",\"receivingTargets\":\"9\"}")));

    assertEquals(new BigDecimal("17.30"), calculator.computePlayerFantasyPoints(2L, "4426388"));
  }

  // ── QB with interceptions and fumbles: Kirk Cousins (ATL vs KC, Week 3) ───
  // passing: 20/29, 230 yds, 1 TD, 1 INT
  // fumbles: 2 fumbles, 0 lost
  // PPR = 230*0.04 + 1*4 + 1*(-2) + 0*(-2) = 9.2 + 4 - 2 + 0 = 11.20
  @Test
  void qbCousinsWeek3WithFumbles() {
    when(repository.findByEventIdAndAthleteEspnId(1L, "2532975"))
        .thenReturn(
            List.of(
                statRow(
                    1L,
                    "2532975",
                    "passing",
                    "{\"completions/passingAttempts\":\"20/29\",\"passingYards\":\"230\",\"yardsPerPassAttempt\":\"7.9\",\"passingTouchdowns\":\"1\",\"interceptions\":\"1\",\"sacks-sackYardsLost\":\"2-1\",\"adjQBR\":\"31.4\",\"QBRating\":\"89.7\"}"),
                statRow(
                    1L,
                    "2532975",
                    "fumbles",
                    "{\"fumbles\":\"2\",\"fumblesLost\":\"0\",\"fumblesRecovered\":\"0\"}")));

    assertEquals(new BigDecimal("11.20"), calculator.computePlayerFantasyPoints(1L, "2532975"));
  }

  // ── QB with lost fumbles ──────────────────────────────────────────────────
  // passing: 300 yds, 2 TD, 0 INT
  // fumbles: 1 fumble, 1 lost
  // PPR = 300*0.04 + 2*4 + 1*(-2) = 12 + 8 - 2 = 18.00
  @Test
  void qbWithLostFumbleDeductsPoints() {
    when(repository.findByEventIdAndAthleteEspnId(3L, "9999999"))
        .thenReturn(
            List.of(
                statRow(
                    3L,
                    "9999999",
                    "passing",
                    "{\"completions/passingAttempts\":\"25/35\",\"passingYards\":\"300\",\"yardsPerPassAttempt\":\"8.6\",\"passingTouchdowns\":\"2\",\"interceptions\":\"0\",\"sacks-sackYardsLost\":\"1-8\",\"adjQBR\":\"70.0\",\"QBRating\":\"110.5\"}"),
                statRow(
                    3L,
                    "9999999",
                    "fumbles",
                    "{\"fumbles\":\"1\",\"fumblesLost\":\"1\",\"fumblesRecovered\":\"0\"}")));

    assertEquals(new BigDecimal("18.00"), calculator.computePlayerFantasyPoints(3L, "9999999"));
  }

  // ── RB dual threat: rushing + receiving + TD in both ──────────────────────
  // rushing: 20 att, 120 yds, 1 TD
  // receiving: 5 rec, 45 yds, 1 TD
  // PPR = 120*0.1 + 1*6 + 5*1 + 45*0.1 + 1*6 = 12 + 6 + 5 + 4.5 + 6 = 33.50
  @Test
  void rbDualThreatRushingAndReceivingTouchdowns() {
    when(repository.findByEventIdAndAthleteEspnId(4L, "8888888"))
        .thenReturn(
            List.of(
                statRow(
                    4L,
                    "8888888",
                    "rushing",
                    "{\"rushingAttempts\":\"20\",\"rushingYards\":\"120\",\"yardsPerRushAttempt\":\"6.0\",\"rushingTouchdowns\":\"1\",\"longRushing\":\"35\"}"),
                statRow(
                    4L,
                    "8888888",
                    "receiving",
                    "{\"receptions\":\"5\",\"receivingYards\":\"45\",\"yardsPerReception\":\"9.0\",\"receivingTouchdowns\":\"1\",\"longReception\":\"18\",\"receivingTargets\":\"7\"}")));

    assertEquals(new BigDecimal("33.50"), calculator.computePlayerFantasyPoints(4L, "8888888"));
  }

  // ── No stats found for athlete → null ─────────────────────────────────────
  @Test
  void returnsNullWhenNoStatsExist() {
    when(repository.findByEventIdAndAthleteEspnId(1L, "0000000")).thenReturn(List.of());

    assertNull(calculator.computePlayerFantasyPoints(1L, "0000000"));
  }

  // ── Only non-scoring categories (e.g. kicking, punting) → null ────────────
  @Test
  void returnsNullWhenOnlyNonScoringCategoriesExist() {
    when(repository.findByEventIdAndAthleteEspnId(1L, "4566192"))
        .thenReturn(
            List.of(
                statRow(
                    1L,
                    "4566192",
                    "kicking",
                    "{\"fieldGoalPct\":\"50.0\",\"longFieldGoalMade\":\"48\",\"totalKickingPoints\":\"5\",\"fieldGoalsMade/fieldGoalAttempts\":\"1/2\",\"extraPointsMade/extraPointAttempts\":\"2/3\"}")));

    assertNull(calculator.computePlayerFantasyPoints(1L, "4566192"));
  }

  // ── Compound stat values like "26/39" are not parseable as numbers → 0 ────
  @Test
  void compoundSlashValuesAreTreatedAsZero() {
    when(repository.findByEventIdAndAthleteEspnId(5L, "1111111"))
        .thenReturn(
            List.of(
                statRow(
                    5L,
                    "1111111",
                    "passing",
                    "{\"completions/passingAttempts\":\"10/20\",\"passingYards\":\"100\",\"passingTouchdowns\":\"0\",\"interceptions\":\"0\"}")));

    // Only passingYards contributes: 100 * 0.04 = 4.00
    assertEquals(new BigDecimal("4.00"), calculator.computePlayerFantasyPoints(5L, "1111111"));
  }

  // ── Dash values like "--" are treated as zero ─────────────────────────────
  @Test
  void dashValuesAreTreatedAsZero() {
    when(repository.findByEventIdAndAthleteEspnId(6L, "2222222"))
        .thenReturn(
            List.of(
                statRow(
                    6L,
                    "2222222",
                    "receiving",
                    "{\"receptions\":\"--\",\"receivingYards\":\"--\",\"receivingTouchdowns\":\"--\"}")));

    assertEquals(new BigDecimal("0.00"), calculator.computePlayerFantasyPoints(6L, "2222222"));
  }

  private static PlayerGameStat statRow(
      Long eventId, String athleteEspnId, String statCategory, String stats) {
    Event event = new Event();
    event.setId(eventId);

    PlayerGameStat row = new PlayerGameStat();
    row.setEvent(event);
    row.setAthleteEspnId(athleteEspnId);
    row.setStatCategory(statCategory);
    row.setStats(stats);
    row.setIngestedAt(Instant.now());
    return row;
  }
}
