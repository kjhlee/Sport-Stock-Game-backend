# Ingestion Service — Developer Reference

## Overview

The **Ingestion Service** is responsible for pulling NFL data from ESPN's public API and writing it into the shared PostgreSQL database (`ingestion` schema). Other services **do not call ESPN directly** — they consume data that this service has already written.

**Tech Stack**: Spring Boot 4.0.3, Spring Data JPA, PostgreSQL, Flyway, Jackson, Java 21

**Port**: `8090` (default, configurable via `INGESTION_SERVICE_PORT`)

There are two categories of endpoints:
- **Orchestrated sync endpoints** — multi-step bulk ingestion jobs that run asynchronously
- **Individual endpoints** — targeted sync or read operations, run synchronously on the request thread

---

## When to Use Which Endpoints

**For scheduled/bulk data loading, use the orchestrated endpoints.** These are the primary way to populate the database at the start of a season, before each week, or for a full refresh.

**For targeted operations or reading data out**, use the individual endpoints. Other services that need to query ingested data (athletes, teams, events, stats) will use the GET endpoints from the individual controllers directly against the database — but if they need to trigger a re-sync of a specific piece of data, the individual POST endpoints are also available.

**Bottom line**: orchestrated endpoints are for "load the data", individual GET endpoints are for "read the data", individual POST endpoints are for "re-sync a specific thing."

---

## Base URL

```
/api/v1/ingestion
```

---

## Orchestrated Sync Endpoints

These run **asynchronously** — the HTTP response returns immediately with `202 Accepted` and the job runs in the background. All three are `POST` requests with query parameters.

### Concurrency rules (important)
- Only **one full sync** can run at a time
- Only **one job per season/type/week window** can run at a time, but different windows can run in parallel
- A full sync and any window job are mutually exclusive

If a job is already running for the same slot, you get `409 Conflict`. If the background thread pool is saturated, you get `503 Service Unavailable`. In both cases, retry later.

---

### `POST /sync/foundation`
Ingests the scoreboard (events + competitors) and all teams for a given week. Run this **before** a weekly sync — it ensures teams and events exist in the DB.

**Params**

| Param | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `seasonYear` | int | ✅ | 2000–2100 | NFL season year (e.g. `2025`) |
| `seasonType` | int | ✅ | 1–4 | 1=preseason, 2=regular, 3=postseason |
| `week` | int | ✅ | 1–25 | Week number |

**Responses**
- `202 Accepted` — job started
- `409 Conflict` — another job is running for this window or a full sync is active
- `503 Service Unavailable` — thread pool is full, retry later

---

### `POST /sync/weekly`
Ingests the scoreboard + all event summaries (boxscore team stats + player game stats) for a given week. Requires teams and events to already exist — run foundation sync first.

**Params** — same as `/sync/foundation`

**Responses** — same as `/sync/foundation`

---

### `POST /sync/full`
Complete ingestion: scoreboard → teams → team details + records → all rosters + coaches → athletes → event summaries. Use this for a full season refresh or initial load.

**Params**

| Param | Type | Required | Default | Constraints | Description |
|---|---|---|---|---|---|
| `seasonYear` | int | ✅ | — | 2000–2100 | |
| `seasonType` | int | ✅ | — | 1–4 | |
| `week` | int | ✅ | — | 1–25 | |
| `rosterLimit` | int | ❌ | `500` | 1–500 | Max players per team roster |
| `athletePageSize` | int | ❌ | `250` | 1–1000 | Athletes per ESPN page |
| `athletePageCount` | int | ❌ | `10` | 1–10000 | Number of pages to fetch |
| `teamEspnIds` | list | ❌ | all teams | max 32 items | Restrict to specific teams (ESPN IDs) |

**Responses** — same conflict/503 pattern; `409` if any window job is active or another full sync is running.

---

## Individual Sync Endpoints (POST)

These run **synchronously** — the request blocks until complete. Useful for re-syncing a specific piece of data without triggering a full job. **These bypass the orchestration mutex**, so be careful not to run them while an orchestrated sync for overlapping data is in progress.

| Endpoint | Description |
|---|---|
| `POST /sync/teams` | Re-sync the full NFL teams list |
| `POST /sync/teams/details?seasonYear=` | Re-sync details + records for all teams |
| `POST /sync/teams/{teamEspnId}?seasonYear=` | Re-sync one team's detail + records |
| `POST /sync/rosters?seasonYear=&rosterLimit=&teamEspnIds=` | Re-sync rosters (all or specific teams) |
| `POST /sync/athletes?pageSize=&pageCount=` | Re-sync athlete list (paginated); defaults: `pageSize=500`, `pageCount=40` |
| `POST /sync/scoreboard?seasonYear=&seasonType=&week=` | Re-sync scoreboard for one week |
| `POST /sync/events/{eventEspnId}/summary` | Re-sync one game's boxscore + player stats |

---

## Read Endpoints (GET)

These are the endpoints other services will call most frequently to consume ingested data.

### Athletes
| Endpoint | Description |
|---|---|
| `GET /athletes` | List all athletes. Optional `?positionAbbreviation=QB` and `?includeStubs=false` filters |
| `GET /athletes/{athleteEspnId}` | Get one athlete by ESPN ID |

### Teams
| Endpoint | Description                                        |
|---|----------------------------------------------------|
| `GET /teams` | List all teams (alphabetical)                      |
| `GET /teams/{teamEspnId}` | Get one team by ESPN ID                            |
| `GET /teams/{teamEspnId}/records?seasonYear=` | Get all win/loss records for a team + season       |
| `GET /teams/{teamEspnId}/records/{recordType}?seasonYear=` | Get a specific record type (`total`, `home`, `road`) |

### Events & Stats
| Endpoint | Description |
|---|---|
| `GET /events?seasonYear=&seasonType=&weekNumber=` | List events for a season year. `seasonType` and `weekNumber` are optional; `seasonType` is required if `weekNumber` is provided |
| `GET /events/{eventEspnId}` | Get one event by ESPN ID |
| `GET /events/{eventEspnId}/team-stats` | Get boxscore team stats for a game |
| `GET /events/{eventEspnId}/player-stats?teamEspnId=` | Get player game stats (optionally filtered by team) |
| `GET /events/{eventEspnId}/player-stats/{athleteEspnId}` | Get all stat categories for a specific athlete in a game |

**Note on player stats**: stats are returned as a `Map<String, String>` in the response, keyed by stat name (e.g. `{"completions":"22","passingYards":"301","touchdowns":"2"}`). The stat keys vary by category (passing, rushing, receiving, etc.). Stats are stored as JSONB in the database.

---

## Response Shapes

### Async job accepted (`202`)
Returned by all `POST /sync/*` endpoints when a job is accepted (orchestrated or individual).
```json
{
  "jobName": "foundationSync",
  "status": "ACCEPTED",
  "requestedAt": "2025-10-01T12:00:00Z"
}
```

### Conflict (`409`)
```json
{
  "jobName": "foundationSync",
  "status": "REJECTED_BUSY",
  "reason": "Another sync job is already running for this season window or a full sync is in progress",
  "requestedAt": "2025-10-01T12:00:00Z"
}
```

### Service unavailable (`503`)
```json
{
  "jobName": "foundationSync",
  "status": "SERVICE_UNAVAILABLE",
  "reason": "Ingestion thread pool is saturated, try again later",
  "requestedAt": "2025-10-01T12:00:00Z"
}
```

---

## Error Responses

All errors follow a consistent shape:

```json
{
  "timestamp": "2025-10-01T12:00:00Z",
  "status": 404,
  "code": "NOT_FOUND",
  "message": "Team not found with ESPN ID: 99"
}
```

| HTTP Status | Code | When |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Invalid request params (list of field errors) |
| 404 | `NOT_FOUND` | ESPN ID doesn't exist in the DB |
| 409 | `REJECTED_BUSY` | Orchestrated sync rejected due to concurrency lock |
| 500 | `INGESTION_ERROR` | ESPN API failure or unexpected ingestion error |
| 501 | `NOT_IMPLEMENTED` | Feature not yet implemented |
| 503 | `SERVICE_UNAVAILABLE` | Thread pool saturated, retry later |

---

## Recommended Workflow (Per Week)

1. `POST /sync/foundation?seasonYear=2025&seasonType=2&week=N` — wait for completion (poll or just allow enough time)
2. `POST /sync/weekly?seasonYear=2025&seasonType=2&week=N` — runs after game results are available (Monday+)
3. For a full season setup: `POST /sync/full` with appropriate params

---

## Key Behaviors to Know

- **ESPN IDs** are numeric strings (1–15 digits), used as the stable external identifier across all entities. Always reference things by `espnId` when calling these endpoints.
- **Upsert semantics**: all sync operations are safe to re-run. They update existing rows rather than duplicating.
- **Rate limiting**: the service throttles ESPN calls internally (200ms delay between requests) — don't worry about it from the outside, but long-running syncs (especially full sync) take time.
- **Async jobs don't push notifications** when they complete — if you need to know when a background sync finishes, poll the relevant GET endpoint or check logs.
- **Pessimistic locking**: Event updates during summary ingestion use `@Lock(PESSIMISTIC_WRITE)` to prevent concurrent modification.
- **Transaction isolation**: Athlete ingestion uses `PROPAGATION_REQUIRES_NEW` with 120s timeout per page to handle large batches.
- **Batch optimization**: Hibernate batching is enabled with batch size 500, ordered inserts/updates for maximum throughput.

---

## Architecture & Key Components

### Data Model (12 JPA Entities)

**Core Entities:**
- **Team** - NFL teams with conference, division, colors, logos
- **Athlete** - Players with position, physical attributes, college, status
- **Event** - Games/matches with date, season, week, status, attendance
- **Season** - Season metadata with start/end dates
- **SeasonWeek** - Individual weeks within seasons

**Relationship Entities:**
- **TeamRecord** - Win/loss records by season and type (total, home, road, division)
- **TeamRosterEntry** - Team-athlete relationships with jersey, position, injury status
- **EventCompetitor** - Team participation in events (home/away, scores, winner)
- **EventCompetitorLinescore** - Period-by-period scoring
- **Coach** - Coaching staff per team per season

**Statistics Entities:**
- **BoxscoreTeamStat** - Team-level game statistics (yards, points, turnovers)
- **PlayerGameStat** - Player statistics stored as JSONB (passing, rushing, receiving, defense)

### Service Layer (7 Services)

1. **IngestionOrchestrationService** - Coordinates multi-step sync jobs with concurrency control
2. **EventIngestionService** - Scoreboard and event data ingestion
3. **TeamIngestionService** - Team details and records
4. **AthleteIngestionService** - Paginated athlete ingestion with deduplication
5. **RosterIngestionService** - Team rosters and coaches
6. **EventSummaryIngestionService** - Game statistics (team and player)
7. **SeasonIngestionService** - Season and week data

### Configuration

**EspnApiProperties** bindings:
```properties
espn.api.site-base-url=https://site.api.espn.com/apis/site/v2
espn.api.core-base-url=https://sports.core.api.espn.com/v2
espn.api.sport=football
espn.api.league=nfl
espn.api.rate-limit-delay-ms=200
espn.api.connect-timeout-seconds=5
espn.api.read-timeout-seconds=60
espn.api.max-athlete-rows-per-sync=50000
```

**AsyncConfig**: Thread pool executor with 2 core threads, 2 max threads, queue capacity 20

### Database Schema (6 Flyway Migrations)

- **V1**: Core tables (teams, athletes, coaches, seasons, season_weeks)
- **V2**: Roster tables (team_records, team_roster_entries)
- **V3**: Event tables (events, event_competitors, event_competitor_linescores)
- **V4**: Statistics tables (boxscore_team_stats, player_game_stats with JSONB)
- **V5**: Performance indexes (single-column on frequently queried fields)
- **V6**: Query path indexes (composite indexes for common join patterns)

**Total: 12 tables, 18+ optimized indexes**

---

## Example Requests & Responses

### POST /api/v1/ingestion/sync/full

**Request:**
```http
POST /api/v1/ingestion/sync/full?seasonYear=2024&seasonType=2&week=5&athletePageSize=250&athletePageCount=10
```

**Response (202 Accepted):**
```json
{
  "jobName": "fullSync",
  "status": "ACCEPTED",
  "requestedAt": "2024-10-15T14:30:00Z"
}
```

---

### GET /api/v1/ingestion/athletes?positionAbbreviation=QB

**Response (200 OK):**
```json
[
  {
    "espnId": "3139477",
    "fullName": "Patrick Mahomes",
    "firstName": "Patrick",
    "lastName": "Mahomes",
    "displayName": "Patrick Mahomes",
    "shortName": "P. Mahomes",
    "positionAbbreviation": "QB",
    "positionDisplayName": "Quarterback",
    "college": "Texas Tech",
    "status": "active",
    "height": 74,
    "weight": 225,
    "jersey": "15",
    "experience": 7,
    "dateOfBirth": "1995-09-17",
    "debutYear": 2017,
    "slug": "patrick-mahomes",
    "headshot": "https://a.espncdn.com/i/headshots/nfl/players/full/3139477.png"
  }
]
```

---

### GET /api/v1/ingestion/events?seasonYear=2024&seasonType=2&weekNumber=5

**Response (200 OK):**
```json
[
  {
    "espnId": "401671754",
    "name": "Kansas City Chiefs at New Orleans Saints",
    "shortName": "KC @ NO",
    "date": "2024-10-07T20:15:00Z",
    "seasonYear": 2024,
    "seasonType": 2,
    "weekNumber": 5,
    "statusTypeId": 3,
    "statusCompleted": true,
    "statusDetail": "Final",
    "attendance": 70123,
    "venueName": "Caesars Superdome",
    "venueCity": "New Orleans",
    "venueState": "LA",
    "broadcast": "ESPN"
  }
]
```

---

### GET /api/v1/ingestion/events/{eventEspnId}/player-stats/{athleteEspnId}

**Response (200 OK):**
```json
[
  {
    "eventEspnId": "401671754",
    "athleteEspnId": "3139477",
    "teamEspnId": "12",
    "statCategory": "passing",
    "stats": {
      "completions": "28",
      "attempts": "39",
      "yards": "331",
      "touchdowns": "4",
      "interceptions": "0",
      "rating": "134.7",
      "sacks": "2",
      "sackYards": "14"
    }
  },
  {
    "eventEspnId": "401671754",
    "athleteEspnId": "3139477",
    "teamEspnId": "12",
    "statCategory": "rushing",
    "stats": {
      "carries": "3",
      "yards": "12",
      "touchdowns": "0",
      "long": "8"
    }
  }
]
```

---

### GET /api/v1/ingestion/teams/{teamEspnId}/records?seasonYear=2024

**Response (200 OK):**
```json
[
  {
    "teamEspnId": "12",
    "seasonYear": 2024,
    "recordType": "total",
    "wins": 8,
    "losses": 1,
    "ties": 0,
    "winPercent": 0.889,
    "pointsFor": 245,
    "pointsAgainst": 178,
    "pointDifferential": 67,
    "divisionWins": 2,
    "divisionLosses": 0,
    "conferenceWins": 5,
    "conferenceLosses": 1,
    "streak": 3,
    "divisionStanding": 1
  },
  {
    "teamEspnId": "12",
    "seasonYear": 2024,
    "recordType": "home",
    "wins": 4,
    "losses": 0,
    "ties": 0,
    "winPercent": 1.000,
    "pointsFor": 128,
    "pointsAgainst": 82
  },
  {
    "teamEspnId": "12",
    "seasonYear": 2024,
    "recordType": "road",
    "wins": 4,
    "losses": 1,
    "ties": 0,
    "winPercent": 0.800,
    "pointsFor": 117,
    "pointsAgainst": 96
  }
]
```

---

## Dev Setup

Set up environment in `infra/`.

Reset and build DB:
```bash
docker compose down -v && docker compose up --build -d
```

Build and run:
```bash
./mvnw -pl services/ingestion clean package && ./mvnw -pl services/ingestion spring-boot:run
```

Default port: `8090`. Connects to Postgres on `localhost:6432`, schema `ingestion`.

