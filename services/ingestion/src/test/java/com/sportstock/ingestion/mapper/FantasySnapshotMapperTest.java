package com.sportstock.ingestion.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests use real ESPN fantasy API response payloads from
 * lm-api-reads.fantasy.espn.com/apis/v3/games/ffl/seasons/2024/players?view=kona_player_info.
 *
 * <p>Source: KC players, 2024 season, scoringPeriodId=1 (Week 1 vs BAL). The stat maps are copied
 * verbatim from the API response (non-zero stats only for readability).
 */
class FantasySnapshotMapperTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  // ═══════════════════════════════════════════════════════════════════════════
  //  ACTUAL fantasy points (statSourceId=0)
  // ═══════════════════════════════════════════════════════════════════════════

  // QB: Patrick Mahomes — actual Week 1 2024 vs BAL
  // 291 pass yds, 1 pass TD, 1 INT, 3 rush yds, 1 rec, 2 rec yds
  // PPR = 291*0.04 + 1*4 + 1*(-2) + 3*0.1 + 1*1 + 2*0.1 = 11.64+4-2+0.3+1+0.2 = 15.14
  @Test
  void qbMahomesActualWeek1() throws Exception {
    JsonNode playerNode =
        OBJECT_MAPPER.readTree(
            """
            {
              "defaultPositionId": 1,
              "stats": [
                {
                  "statSourceId": 0,
                  "scoringPeriodId": 1,
                  "stats": {
                    "0": 28.0,
                    "1": 20.0,
                    "3": 291.0,
                    "4": 1.0,
                    "20": 1.0,
                    "23": 2.0,
                    "24": 3.0,
                    "41": 1.0,
                    "42": 2.0,
                    "53": 1.0,
                    "58": 1.0
                  }
                }
              ]
            }
            """);

    assertEquals(
        new BigDecimal("15.14"), FantasySnapshotMapper.extractActualFantasyPoints(playerNode, 1));
  }

  // RB: Isiah Pacheco — actual Week 1 2024 vs BAL
  // 45 rush yds, 1 rush TD, 2 rec, 33 rec yds
  // PPR = 45*0.1 + 1*6 + 2*1 + 33*0.1 = 4.5+6+2+3.3 = 15.80
  @Test
  void rbPachecoActualWeek1() throws Exception {
    JsonNode playerNode =
        OBJECT_MAPPER.readTree(
            """
            {
              "defaultPositionId": 2,
              "stats": [
                {
                  "statSourceId": 0,
                  "scoringPeriodId": 1,
                  "stats": {
                    "23": 15.0,
                    "24": 45.0,
                    "25": 1.0,
                    "41": 2.0,
                    "42": 33.0,
                    "53": 2.0,
                    "58": 3.0
                  }
                }
              ]
            }
            """);

    assertEquals(
        new BigDecimal("15.80"), FantasySnapshotMapper.extractActualFantasyPoints(playerNode, 1));
  }

  // WR: Rashee Rice — actual Week 1 2024 vs BAL
  // 7 rec, 103 rec yds, 0 TD
  // PPR = 7*1 + 103*0.1 = 7+10.3 = 17.30
  @Test
  void wrRiceActualWeek1() throws Exception {
    JsonNode playerNode =
        OBJECT_MAPPER.readTree(
            """
            {
              "defaultPositionId": 3,
              "stats": [
                {
                  "statSourceId": 0,
                  "scoringPeriodId": 1,
                  "stats": {
                    "41": 7.0,
                    "42": 103.0,
                    "53": 7.0,
                    "58": 9.0
                  }
                }
              ]
            }
            """);

    assertEquals(
        new BigDecimal("17.30"), FantasySnapshotMapper.extractActualFantasyPoints(playerNode, 1));
  }

  // TE: Travis Kelce — actual Week 1 2024 vs BAL
  // 3 rec, 34 rec yds, 0 TD
  // PPR = 3*1 + 34*0.1 = 3+3.4 = 6.40
  @Test
  void teKelceActualWeek1() throws Exception {
    JsonNode playerNode =
        OBJECT_MAPPER.readTree(
            """
            {
              "defaultPositionId": 4,
              "stats": [
                {
                  "statSourceId": 0,
                  "scoringPeriodId": 1,
                  "stats": {
                    "41": 3.0,
                    "42": 34.0,
                    "53": 3.0,
                    "58": 4.0
                  }
                }
              ]
            }
            """);

    assertEquals(
        new BigDecimal("6.40"), FantasySnapshotMapper.extractActualFantasyPoints(playerNode, 1));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //  PROJECTED fantasy points (statSourceId=1)
  // ═══════════════════════════════════════════════════════════════════════════

  // QB: Patrick Mahomes — projected Week 1 2024
  // Key stats: 267.13 pass yds, 2.039 pass TD, 0.067 pass 2pt, 0.798 INT,
  //            21.534 rush yds, 0.062 rush TD, 0.003 rush 2pt,
  //            0.00212 fumble rec TD, 0.231 lost fumbles
  // No receiving stats (stat 41/42/53 absent)
  @Test
  void qbMahomesProjectedWeek1() throws Exception {
    JsonNode playerNode =
        OBJECT_MAPPER.readTree(
            """
            {
              "defaultPositionId": 1,
              "stats": [
                {
                  "statSourceId": 1,
                  "scoringPeriodId": 1,
                  "stats": {
                    "0": 37.70793903,
                    "1": 25.01282024,
                    "3": 267.1316344,
                    "4": 2.03924968,
                    "19": 0.067008899,
                    "20": 0.797844429,
                    "23": 4.132280017,
                    "24": 21.5338189,
                    "25": 0.062464773,
                    "26": 0.003288992,
                    "62": 0.070297891,
                    "63": 0.00212,
                    "68": 0.482669998,
                    "72": 0.230772268
                  }
                }
              ]
            }
            """);

    // passingYards:    267.1316344 * 0.04 = 10.69
    // passingTD:       2.03924968 * 4     = 8.16
    // passing2pt:      0.067008899 * 2    = 0.13
    // passingINT:      0.797844429 * -2   = -1.60
    // rushingYards:    21.5338189 * 0.1   = 2.15
    // rushingTD:       0.062464773 * 6    = 0.37
    // rushing2pt:      0.003288992 * 2    = 0.01
    // receptions:      0 (no stat 53/41)  = 0.00
    // receivingYards:  0 (no stat 42)     = 0.00
    // receivingTD:     0                  = 0.00
    // receiving2pt:    0                  = 0.00
    // fumbleRecTD:     0.00212 * 6        = 0.01
    // lostFumbles:     0.230772268 * -2   = -0.46
    // Total: 19.46
    assertEquals(
        new BigDecimal("19.46"),
        FantasySnapshotMapper.extractProjectedFantasyPoints(playerNode, 1));
  }

  // RB: Isiah Pacheco — projected Week 1 2024
  @Test
  void rbPachecoProjectedWeek1() throws Exception {
    JsonNode playerNode =
        OBJECT_MAPPER.readTree(
            """
            {
              "defaultPositionId": 2,
              "stats": [
                {
                  "statSourceId": 1,
                  "scoringPeriodId": 1,
                  "stats": {
                    "23": 14.3692677,
                    "24": 60.65868903,
                    "25": 0.522494816,
                    "26": 0.027511204,
                    "42": 20.50750668,
                    "43": 0.141371099,
                    "44": 0.004645396,
                    "53": 3.254544856,
                    "58": 3.946620287,
                    "63": 0.000753,
                    "72": 0.081879947
                  }
                }
              ]
            }
            """);

    // passingYards:    0                        = 0.00
    // passingTD:       0                        = 0.00
    // passing2pt:      0                        = 0.00
    // passingINT:      0                        = 0.00
    // rushingYards:    60.65868903 * 0.1        = 6.07
    // rushingTD:       0.522494816 * 6          = 3.13
    // rushing2pt:      0.027511204 * 2          = 0.06
    // receptions:      3.254544856 * 1 (stat53) = 3.25
    // receivingYards:  20.50750668 * 0.1        = 2.05
    // receivingTD:     0.141371099 * 6          = 0.85
    // receiving2pt:    0.004645396 * 2          = 0.01
    // fumbleRecTD:     0.000753 * 6             = 0.00 (rounds to 0.00)
    // lostFumbles:     0.081879947 * -2         = -0.16
    // Total: 15.26
    assertEquals(
        new BigDecimal("15.26"),
        FantasySnapshotMapper.extractProjectedFantasyPoints(playerNode, 1));
  }

  // WR: Rashee Rice — projected Week 1 2024
  @Test
  void wrRiceProjectedWeek1() throws Exception {
    JsonNode playerNode =
        OBJECT_MAPPER.readTree(
            """
            {
              "defaultPositionId": 3,
              "stats": [
                {
                  "statSourceId": 1,
                  "scoringPeriodId": 1,
                  "stats": {
                    "23": 0.104125128,
                    "24": 0.588134898,
                    "25": 0.00494291,
                    "26": 0.000260262,
                    "42": 60.2601885,
                    "43": 0.482031038,
                    "44": 0.01583934,
                    "53": 5.085316803,
                    "58": 7.175673248,
                    "63": 0.000457,
                    "72": 0.057913499
                  }
                }
              ]
            }
            """);

    // passingYards:    0                        = 0.00
    // passingTD:       0                        = 0.00
    // passing2pt:      0                        = 0.00
    // passingINT:      0                        = 0.00
    // rushingYards:    0.588134898 * 0.1        = 0.06
    // rushingTD:       0.00494291 * 6           = 0.03
    // rushing2pt:      0.000260262 * 2          = 0.00
    // receptions:      5.085316803 * 1 (stat53) = 5.09
    // receivingYards:  60.2601885 * 0.1         = 6.03
    // receivingTD:     0.482031038 * 6          = 2.89
    // receiving2pt:    0.01583934 * 2           = 0.03
    // fumbleRecTD:     0.000457 * 6             = 0.00
    // lostFumbles:     0.057913499 * -2         = -0.12
    // Total: 14.01
    assertEquals(
        new BigDecimal("14.01"),
        FantasySnapshotMapper.extractProjectedFantasyPoints(playerNode, 1));
  }

  // TE: Travis Kelce — projected Week 1 2024
  @Test
  void teKelceProjectedWeek1() throws Exception {
    JsonNode playerNode =
        OBJECT_MAPPER.readTree(
            """
            {
              "defaultPositionId": 4,
              "stats": [
                {
                  "statSourceId": 1,
                  "scoringPeriodId": 1,
                  "stats": {
                    "23": 0.104125128,
                    "24": 0.014166086,
                    "25": 0.002114177,
                    "26": 0.000111319,
                    "42": 57.42641698,
                    "43": 0.456765891,
                    "44": 0.015009138,
                    "53": 5.680200487,
                    "58": 7.534456911,
                    "63": 0.000258,
                    "72": 0.032709169
                  }
                }
              ]
            }
            """);

    // rushingYards:    0.014166086 * 0.1        = 0.00
    // rushingTD:       0.002114177 * 6          = 0.01
    // rushing2pt:      0.000111319 * 2          = 0.00
    // receptions:      5.680200487 * 1 (stat53) = 5.68
    // receivingYards:  57.42641698 * 0.1        = 5.74
    // receivingTD:     0.456765891 * 6          = 2.74
    // receiving2pt:    0.015009138 * 2          = 0.03
    // fumbleRecTD:     0.000258 * 6             = 0.00
    // lostFumbles:     0.032709169 * -2         = -0.07
    // Total: 14.13
    assertEquals(
        new BigDecimal("14.13"),
        FantasySnapshotMapper.extractProjectedFantasyPoints(playerNode, 1));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //  Kicker is now excluded — position ID 5 should not produce snapshots.
  //  If a kicker node somehow reaches the mapper, it gets scored as offense
  //  (all offense stats are 0, so the result is 0.00).
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  void kickerReturnsNullSinceUnsupported() throws Exception {
    JsonNode playerNode =
        OBJECT_MAPPER.readTree(
            """
            {
              "defaultPositionId": 5,
              "stats": [
                {
                  "statSourceId": 0,
                  "scoringPeriodId": 1,
                  "stats": {
                    "80": 2.0,
                    "81": 2.0,
                    "83": 2.0,
                    "84": 2.0,
                    "86": 3.0,
                    "87": 3.0
                  }
                }
              ]
            }
            """);

    assertNull(FantasySnapshotMapper.extractActualFantasyPoints(playerNode, 1));
    assertNull(FantasySnapshotMapper.extractProjectedFantasyPoints(playerNode, 1));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //  DST returns null
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  void dstReturnsNull() throws Exception {
    JsonNode playerNode =
        OBJECT_MAPPER.readTree(
            """
            {
              "defaultPositionId": 16,
              "stats": [
                {
                  "statSourceId": 0,
                  "scoringPeriodId": 1,
                  "stats": {
                    "95": 2.0,
                    "99": 3.0,
                    "120": 14.0
                  }
                }
              ]
            }
            """);

    assertNull(FantasySnapshotMapper.extractActualFantasyPoints(playerNode, 1));
    assertNull(FantasySnapshotMapper.extractProjectedFantasyPoints(playerNode, 1));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //  appliedTotal is used when present
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  void usesAppliedTotalWhenPresent() throws Exception {
    JsonNode playerNode =
        OBJECT_MAPPER.readTree(
            """
            {
              "defaultPositionId": 2,
              "stats": [
                {
                  "statSourceId": 0,
                  "scoringPeriodId": 1,
                  "appliedTotal": 22.5,
                  "stats": {
                    "24": 100.0,
                    "25": 1.0
                  }
                }
              ]
            }
            """);

    assertEquals(
        new BigDecimal("22.50"), FantasySnapshotMapper.extractActualFantasyPoints(playerNode, 1));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //  Missing scoring period returns null
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  void returnsNullWhenScoringPeriodNotFound() throws Exception {
    JsonNode playerNode =
        OBJECT_MAPPER.readTree(
            """
            {
              "defaultPositionId": 2,
              "stats": [
                {
                  "statSourceId": 0,
                  "scoringPeriodId": 5,
                  "stats": { "24": 100.0 }
                }
              ]
            }
            """);

    assertNull(FantasySnapshotMapper.extractActualFantasyPoints(playerNode, 1));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //  Projection receiving uses stat 53 for receptions (not stat 41)
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  void projectionUsesstat53ForReceptionsNotStat41() throws Exception {
    JsonNode playerNode =
        OBJECT_MAPPER.readTree(
            """
            {
              "defaultPositionId": 4,
              "stats": [
                {
                  "statSourceId": 1,
                  "scoringPeriodId": 5,
                  "stats": {
                    "42": 69.8171063,
                    "43": 0.637758277,
                    "53": 4.882189207,
                    "58": 8.491607476,
                    "72": 0.038598244
                  }
                }
              ]
            }
            """);

    // receptions = stat 53 = 4.882189207 * 1 = 4.88
    // recYards = 69.8171063 * 0.1 = 6.98
    // recTD = 0.637758277 * 6 = 3.83
    // lostFumbles = 0.038598244 * -2 = -0.08
    // Total: 4.88 + 6.98 + 3.83 + (-0.08) = 15.61
    assertEquals(
        new BigDecimal("15.61"),
        FantasySnapshotMapper.extractProjectedFantasyPoints(playerNode, 5));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //  Actual receiving uses stat 41 for receptions (not stat 53)
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  void actualUsesStat41ForReceptionsNotStat53() throws Exception {
    JsonNode playerNode =
        OBJECT_MAPPER.readTree(
            """
            {
              "defaultPositionId": 2,
              "stats": [
                {
                  "statSourceId": 0,
                  "scoringPeriodId": 5,
                  "stats": {
                    "23": 22.0,
                    "24": 57.0,
                    "41": 8.0,
                    "42": 82.0,
                    "43": 1.0
                  }
                }
              ]
            }
            """);

    // rushYards = 57*0.1 = 5.70, receptions = stat41 = 8*1 = 8.00,
    // recYards = 82*0.1 = 8.20, recTD = 1*6 = 6.00
    // Total: 5.70 + 8.00 + 8.20 + 6.00 = 27.90
    assertEquals(
        new BigDecimal("27.90"), FantasySnapshotMapper.extractActualFantasyPoints(playerNode, 5));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  //  Displayable stats normalization for projections
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  void normalizesProjectedReceivingStatsForStorage() throws Exception {
    JsonNode playerNode =
        OBJECT_MAPPER.readTree(
            """
            {
              "defaultPositionId": 4,
              "stats": [
                {
                  "statSourceId": 1,
                  "scoringPeriodId": 5,
                  "stats": {
                    "42": 69.8171063,
                    "43": 0.637758277,
                    "53": 4.882189207,
                    "58": 8.491607476,
                    "72": 0.038598244
                  }
                }
              ]
            }
            """);

    @SuppressWarnings("unchecked")
    var explanation =
        (Map<String, Object>) FantasySnapshotMapper.explainFantasyPoints(playerNode, 1, 5);
    @SuppressWarnings("unchecked")
    var normalizedStats = (Map<String, BigDecimal>) explanation.get("normalizedStats");

    assertEquals(new BigDecimal("4.882189207"), normalizedStats.get("receptions"));
    assertEquals(new BigDecimal("69.8171063"), normalizedStats.get("receivingYards"));
    assertEquals(new BigDecimal("0.637758277"), normalizedStats.get("receivingTouchdowns"));
    assertEquals(new BigDecimal("8.491607476"), normalizedStats.get("receivingTargets"));
    assertEquals(new BigDecimal("0.038598244"), normalizedStats.get("lostFumbles"));
  }
}
