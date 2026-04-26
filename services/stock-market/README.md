# Stock Market Service

Spring Boot service that manages player stocks, pricing, and price history for the Sport Stock Game.

- **Schema:** `market`
- **Port:** `8130`

## Endpoints

### Stocks

| Method | Path                                     | Description                                     |
| ------ | ---------------------------------------- | ----------------------------------------------- |
| `GET`  | `/api/v1/stocks`                         | List stocks (paginated)                         |
| `GET`  | `/api/v1/stocks/{stockId}`               | Get a single stock by ID                        |
| `GET`  | `/api/v1/stocks/{stockId}/price-history` | Get week-by-week price history for a stock      |
| `POST` | `/api/internal/stocks/sync-athletes`     | Sync player stocks from the ingestion service   |
| `POST` | `/api/internal/stocks/update-projected-prices` | Update projected prices for a given week |

---

### `GET /api/v1/stocks`

Query params:

| Param      | Required | Description                                         |
| ---------- | -------- | --------------------------------------------------- |
| `position` | No       | Filter by position (`QB`, `RB`, `WR`, `TE`, `K`)    |
| `status`   | No       | Filter by status (`ACTIVE`, `INACTIVE`, `DELISTED`) |
| `page`     | No       | Page index, default `0`                             |
| `size`     | No       | Page size, default `20`                             |

```bash
curl "http://localhost:8130/api/v1/stocks?position=QB&status=ACTIVE"
```

---

### `GET /api/v1/stocks/{stockId}`

```bash
curl "http://localhost:8130/api/v1/stocks/<uuid>"
```

---

### `GET /api/v1/stocks/{stockId}/price-history`

Returns week-by-week prices ordered by week ascending. Each entry includes `priceType`, such as `BASE` for projection-based prices and `FINAL` for final postgame prices.

Query params:

| Param        | Required | Description                                      |
| ------------ | -------- | ------------------------------------------------ |
| `seasonYear` | Yes      | e.g. `2024`                                      |
| `seasonType` | Yes      | `1` = preseason, `2` = regular, `3` = postseason |

```bash
curl "http://localhost:8130/api/v1/stocks/<uuid>/price-history?seasonYear=2024&seasonType=2"
```

---

### `POST /api/internal/stocks/sync-athletes`

Pulls athletes from the ingestion service and creates/updates `Stock` records. Assigns base prices by position on first creation.

Query params:

| Param      | Required | Description                                              |
| ---------- | -------- | -------------------------------------------------------- |
| `position` | No       | Limit sync to one position (`QB`, `RB`, `WR`, `TE`, `K`) |

```bash
# sync all positions
curl -X POST "http://localhost:8130/api/internal/stocks/sync-athletes"

# sync QBs only
curl -X POST "http://localhost:8130/api/internal/stocks/sync-athletes?position=QB"
```

---

### `POST /api/internal/stocks/update-projected-prices`

Fetches completed game stats from ingestion for the given week, computes a fantasy-points-based performance score per player, applies exponential smoothing to their current price, and saves a `PriceHistory` record.

**Pricing formula:** `newPrice = α × performanceScore + (1 − α) × currentPrice`, clamped to `priceFloor`.

Intended to be called by the scheduling service after each week's games are complete.

Query params:

| Param        | Required | Description                                      |
| ------------ | -------- | ------------------------------------------------ |
| `seasonYear` | Yes      | e.g. `2024`                                      |
| `seasonType` | Yes      | `1` = preseason, `2` = regular, `3` = postseason |
| `weekNumber` | Yes      | Week number within the season                    |

```bash
curl -X POST "http://localhost:8130/api/internal/stocks/update-projected-prices?seasonYear=2024&seasonType=2&weekNumber=1"
```

---

## Pricing config

Configured in `application.properties` under the `pricing.*` namespace:

| Property                  | Default | Description                          |
| ------------------------- | ------- | ------------------------------------ |
| `pricing.smoothing-alpha` | `0.6`   | EMA weight for new performance score |
| `pricing.price-floor`     | `1.00`  | Minimum price a stock can fall to    |
| `pricing.base-prices.QB`  | `15.00` | Initial price for QBs                |
| `pricing.base-prices.RB`  | `12.00` | Initial price for RBs                |
| `pricing.base-prices.WR`  | `10.00` | Initial price for WRs                |
| `pricing.base-prices.TE`  | `8.00`  | Initial price for TEs                |
| `pricing.base-prices.K`   | `5.00`  | Initial price for Ks                 |

---

## End-to-end walkthrough

This section walks through the full flow from a fresh database to querying price history.

### Step 1 — Start the database and services

```bash
# From the repo root
docker compose -f docker-compose.yml up -d

# Start ingestion (port 8090)
mvn -f services/ingestion/pom.xml spring-boot:run

# Start stock-market (port 8130)
mvn -f services/stock-market/pom.xml spring-boot:run
```

### Step 2 — Populate the ingestion database

Pull teams, rosters, and the NFL 2024 regular season week 1 schedule:

```bash
curl -X POST "http://localhost:8090/api/internal/ingestion/sync/teams"
curl -X POST "http://localhost:8090/api/internal/ingestion/sync/teams/details?seasonYear=2024"
curl -X POST "http://localhost:8090/api/internal/ingestion/sync/rosters?seasonYear=2024&rosterLimit=500"
curl -X POST "http://localhost:8090/api/internal/ingestion/sync/scoreboard?seasonYear=2024&seasonType=2&week=1"
```

### Step 3 — Sync player stocks

Creates a `Stock` record for every athlete in ingestion. Run this once (or again after a new roster sync):

```bash
curl -X POST "http://localhost:8130/api/internal/stocks/sync-athletes"
```

Example response:
```json
{
  "created": 312,
  "updated": 0,
  "skipped": 0,
  "totalFetched": 312
}
```

### Step 4 — Update prices for a week

Fetches completed game stats from ingestion, computes a performance score per player, and writes a `PriceHistory` record. Safe to call multiple times for the same week — it will update the existing record rather than insert a duplicate.

```bash
curl -X POST "http://localhost:8130/api/internal/stocks/update-projected-prices?seasonYear=2024&seasonType=2&weekNumber=1"
```

Example response:
```json
{
  "updated": 248,
  "skipped": 64
}
```

`skipped` counts athletes in ingestion that have no matching `Stock` (e.g. positions not tracked).

Repeat for additional weeks to build up history:

```bash
curl -X POST "http://localhost:8130/api/internal/stocks/update-projected-prices?seasonYear=2024&seasonType=2&weekNumber=2"
curl -X POST "http://localhost:8130/api/internal/stocks/update-projected-prices?seasonYear=2024&seasonType=2&weekNumber=3"
```

### Step 5 — Browse stocks

List all active QBs:

```bash
curl "http://localhost:8130/api/v1/stocks?position=QB&status=ACTIVE"
```

Example response:
```json
{
  "stocks": [
    {
      "id": "c7e94ef9-18f5-4920-9788-e59a95753daa",
      "athleteEspnId": "3139477",
      "fullName": "Lamar Jackson",
      "position": "QB",
      "teamEspnId": "33",
      "currentPrice": 22.45,
      "status": "ACTIVE",
      "priceUpdatedAt": "2024-09-10T02:15:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 36,
  "totalPages": 2
}
```

Copy the `id` from any stock to use in the next steps.

### Step 6 — Get a single stock

```bash
curl "http://localhost:8130/api/v1/stocks/c7e94ef9-18f5-4920-9788-e59a95753daa"
```

Example response:
```json
{
  "id": "c7e94ef9-18f5-4920-9788-e59a95753daa",
  "athleteEspnId": "3139477",
  "fullName": "Lamar Jackson",
  "position": "QB",
  "teamEspnId": "33",
  "currentPrice": 22.45,
  "status": "ACTIVE",
  "priceUpdatedAt": "2024-09-10T02:15:00Z"
}
```

### Step 7 — Get price history

Returns one entry per week, ordered by week ascending:

```bash
curl "http://localhost:8130/api/v1/stocks/c7e94ef9-18f5-4920-9788-e59a95753daa/price-history?seasonYear=2024&seasonType=2"
```

Example response:
```json
[
  {
    "seasonYear": 2024,
    "seasonType": 2,
    "week": 1,
    "price": 18.12,
    "priceType": "BASE",
    "recordedAt": "2024-09-10T02:15:00Z"
  },
  {
    "seasonYear": 2024,
    "seasonType": 2,
    "week": 2,
    "price": 20.87,
    "priceType": "FINAL",
    "recordedAt": "2024-09-17T02:30:00Z"
  },
  {
    "seasonYear": 2024,
    "seasonType": 2,
    "week": 3,
    "price": 22.45,
    "priceType": "BASE",
    "recordedAt": "2024-09-24T02:10:00Z"
  }
]
```

---

## Resetting the database

```bash
docker compose -f docker-compose.yml down -v
docker compose -f docker-compose.yml up -d
```

## Accessing the DB

```bash
psql -h localhost -p 6432 -U sportstock -d sportstock
```

## Testing
To run the tests:
```bash
mvn -f services/stock-market/pom.xml test
```
To run a single test class:
```bash
mvn -f services/stock-market/pom.xml test -Dtest=PricingServiceTest
```
