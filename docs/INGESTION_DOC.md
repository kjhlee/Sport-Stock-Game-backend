# Ingestion Service — Developer Reference

## Overview

The ingestion service is responsible for pulling NFL data from ESPN's public API and writing it into the shared Postgres database. Other services **do not call ESPN directly** — they consume data that this service has already written.

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
- **Rate limiting**: the service throttles ESPN calls internally — don't worry about it from the outside, but long-running syncs (especially full sync) take time.
- **Async jobs don't push notifications** when they complete — if you need to know when a background sync finishes, poll the relevant GET endpoint or check logs.

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

Default port: `8081`. Connects to Postgres on `localhost:6432`, schema `ingestion`.

