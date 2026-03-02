# Stock Market Service — Context

## Project Overview

This is a Spring Boot microservice that manages NFL player stock prices. It is part of a multi-service monorepo. The ingestion service (already built) pulls NFL data from ESPN into a shared Postgres database. This service reads that data and calculates player stock prices.

## Tech Stack

- Java 21, Spring Boot, Maven
- PostgreSQL (shared with ingestion service)
- Flyway for migrations
- Spring Data JPA / Hibernate

## Schemas

- `market` — owned by this service (player_stock, price_history)
- `ingestion` — owned by the ingestion service (read-only access)
- Schema creation is handled by `infra/01-create-schemas.sql`

## Build & Run

```bash
# Database (shared with ingestion service)
docker compose -f infra/docker-compose.yml down -v && docker compose -f infra/docker-compose.yml up --build -d

# Build
./mvnw -pl services/stock-market clean package

# Run
./mvnw -pl services/stock-market spring-boot:run

```

Ingestion service runs on port `8081`. Stock market service runs on port `8082`.

## Conventions & Rules

- **Timestamps:** All `TIMESTAMP` columns use `TIMESTAMPTZ` in Postgres. Java entities use `Instant` — never `LocalDateTime`.
- **Money:** `DECIMAL(10,2)` in Postgres, `BigDecimal` in Java — never `float` or `double`.
- **Stat parsing:** ESPN stat values are JSON strings that may be null, empty, or non-numeric. Always default to `BigDecimal.ZERO`:

```java
private BigDecimal parseStat(Map<String, String> stats, String key) {
    String value = stats.get(key);
    if (value == null || value.isBlank()) return BigDecimal.ZERO;
    try {
        return new BigDecimal(value);
    } catch (NumberFormatException e) {
        return BigDecimal.ZERO;
    }
}
```

---

## Database Schema (Owned — `market`)

### `player_stock`

```sql
CREATE TABLE market.player_stock (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    athlete_espn_id  VARCHAR(15)    NOT NULL UNIQUE,
    full_name        VARCHAR(255)   NOT NULL,
    position         VARCHAR(10)    NOT NULL,
    team_espn_id     VARCHAR(15),
    current_price    DECIMAL(10,2)  NOT NULL DEFAULT 0,
    status           VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    price_updated_at TIMESTAMPTZ,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now()
);
```

`status` enum values: `ACTIVE`, `SUSPENDED`, `DELISTED`

### `price_history`

One row per player per week. Unique index enforces this and doubles as the query index.

```sql
CREATE TABLE market.price_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_stock_id UUID           NOT NULL REFERENCES market.player_stock(id),
    season_year     INT            NOT NULL,
    season_type     INT            NOT NULL,
    week            INT            NOT NULL,
    price           DECIMAL(10,2)  NOT NULL,
    recorded_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_price_history_unique ON market.price_history(player_stock_id, season_year, season_type, week);
```

---

## Ingestion Service (Read-Only — `ingestion` schema)

This service reads from the ingestion service's tables. Map these as **read-only** JPA entities with `@Immutable` in `repository/ingestion/`. No save/update methods. Use `@Table(name = "...", schema = "ingestion")`.

### Tables and columns needed

**`ingestion.athletes`** — ESPN ID (`espn_id`), full name (`full_name`), position (`position_abbreviation`), team via roster entries

**`ingestion.events`** — ESPN ID (`espn_id`), season year (`season_year`), season type (`season_type`), week (`week_number`), completion status (`status_completed`)

**`ingestion.player_game_stats`** — athlete ESPN ID (`athlete_espn_id`), event ID (`event_id`), team ID (`team_id`), stat category (`stat_category`), stats JSON (`stats`)

**`ingestion.team_roster_entries`** — links athletes to teams for a given season (`team_id`, `athlete_id`, `season_year`)

**`ingestion.teams`** — team ESPN ID (`espn_id`), display name (`display_name`), abbreviation (`abbreviation`)

### Stats field

The `stats` column in `player_game_stats` is **JSONB** keyed by stat name, e.g. `{"completions":"22","passingYards":"301"}`. Each row has a `stat_category` (e.g., `passing`, `rushing`, `receiving`), so a player may have multiple rows per game. The exact stat keys must be confirmed:

```sql
SELECT DISTINCT jsonb_object_keys(stats) FROM ingestion.player_game_stats LIMIT 100;
```

### Ingestion REST endpoints (for reference)

| Endpoint                                                              | Description                                        |
| --------------------------------------------------------------------- | -------------------------------------------------- |
| `GET /api/v1/ingestion/athletes`                                      | List athletes. Optional `?positionAbbreviation=QB` |
| `GET /api/v1/ingestion/athletes/{athleteEspnId}`                      | Single athlete                                     |
| `GET /api/v1/ingestion/events?seasonYear=&weekNumber=`                | Events for a season/week                           |
| `GET /api/v1/ingestion/events/{eventEspnId}/player-stats?teamEspnId=` | Player game stats                                  |

---

## Pricing - TBD (temporary)

### PPR multipliers

```java
private static final Map<String, BigDecimal> PPR_MULTIPLIERS = Map.ofEntries(
    entry("passingYards",       new BigDecimal("0.04")),
    entry("passingTouchdowns",  new BigDecimal("4")),
    entry("interceptions",      new BigDecimal("-2")),
    entry("rushingYards",       new BigDecimal("0.1")),
    entry("rushingTouchdowns",  new BigDecimal("6")),
    entry("receptions",         new BigDecimal("1")),
    entry("receivingYards",     new BigDecimal("0.1")),
    entry("receivingTouchdowns",new BigDecimal("6")),
    entry("fumblesLost",        new BigDecimal("-2")),
    entry("fieldGoalsMade",     new BigDecimal("3")),
    entry("extraPointsMade",    new BigDecimal("1"))
);
```

### Formula 

```
fantasy_points = Σ(stat_value × ppr_multiplier)
smoothed_price = (ALPHA × fantasy_points) + ((1 - ALPHA) × current_price)
final_price    = max(PRICE_FLOOR, smoothed_price)
```

### Recalculation flow

1. Read completed events for the given week from `ingestion.events` (`status_completed = true`)
2. Read player stats for those events from `ingestion.player_game_stats`
3. For each active `PlayerStock`:
   - Look up stats for this week (empty if bye/inactive)
   - A player may have multiple `player_game_stats` rows per game (one per `stat_category`) — merge them
   - Calculate fantasy points using PPR multipliers
   - Smooth against current price
   - Enforce price floor
   - Update `market.player_stock.current_price` and `price_updated_at`
   - Insert `market.price_history` row
4. If no completed events found, return `stocksUpdated: 0` — do not fail

---

## REST API

Base path: `/api/v1/stocks`

### `GET /api/v1/stocks`

**Controller:** `StockController`

| Param      | Type   | Default      |
| ---------- | ------ | ------------ |
| `position` | String | all          |
| `status`   | String | ACTIVE       |
| `page`     | int    | 0            |
| `size`     | int    | 20 (max 100) |

```json
// 200
{
  "content": [
    {
      "id": "uuid",
      "athleteEspnId": "3139477",
      "fullName": "Patrick Mahomes",
      "position": "QB",
      "teamEspnId": "12",
      "currentPrice": 28.50,
      "status": "ACTIVE",
      "priceUpdatedAt": "2025-10-14T06:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 487,
  "totalPages": 25
}
```

### `GET /api/v1/stocks/{stockId}`

**Controller:** `StockController`

Returns single stock object (same shape as list item). `404` if not found.

### `GET /api/v1/stocks/athlete/{athleteEspnId}`

**Controller:** `StockController`

Look up by ESPN ID. Same response shape.

### `POST /api/v1/stocks/recalculate`

**Controller:** `PricingAdminController`

Synchronous. Called by orchestration service after ingestion completes.

```json
// Request
{ "seasonYear": 2025, "seasonType": 2, "week": 6 }

// 200
{ "stocksUpdated": 487, "errors": 2, "errorDetails": "Missing stats for ESPN IDs: 12345, 67890" }
```

### `POST /api/v1/stocks/sync-athletes`

**Controller:** `PricingAdminController`

| Param      | Type   | Default |
| ---------- | ------ | ------- |
| `position` | String | all     |

```json
// 200
{ "created": 42, "updated": 445, "total": 487 }
```

### Error format

All endpoints return errors in this shape:

```json
{
  "timestamp": "2025-10-14T06:00:00Z",
  "status": 404,
  "code": "NOT_FOUND",
  "message": "Stock not found with ID: uuid"
}
```

| Status | Code               |
| ------ | ------------------ |
| 400    | `VALIDATION_ERROR` |
| 404    | `NOT_FOUND`        |
| 500    | `INTERNAL_ERROR`   |

---

## Project Structure

```
services/stock-market/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/yourapp/stockmarket/
    │   │   ├── StockMarketApplication.java
    │   │   │
    │   │   ├── config/
    │   │   │   └── PricingConfig.java
    │   │   │
    │   │   ├── controller/
    │   │   │   ├── StockController.java
    │   │   │   ├── PricingAdminController.java
    │   │   │   └── advice/
    │   │   │       └── GlobalExceptionHandler.java
    │   │   │
    │   │   ├── service/
    │   │   │   ├── StockService.java
    │   │   │   ├── PricingService.java
    │   │   │   ├── PriceRecalculationService.java
    │   │   │   ├── AthleteStockSyncService.java
    │   │   │   └── IngestionDataService.java
    │   │   │
    │   │   ├── model/
    │   │   │   ├── entity/
    │   │   │   │   ├── PlayerStock.java
    │   │   │   │   └── PriceHistory.java
    │   │   │   ├── dto/
    │   │   │   │   ├── request/
    │   │   │   │   │   └── RecalculateRequest.java
    │   │   │   │   └── response/
    │   │   │   │       ├── StockResponse.java
    │   │   │   │       ├── RecalculateResponse.java
    │   │   │   │       ├── SyncResultResponse.java
    │   │   │   │       └── ErrorResponse.java
    │   │   │   ├── enums/
    │   │   │   │   └── StockStatus.java
    │   │   │   └── mapper/
    │   │   │       └── StockMapper.java
    │   │   │
    │   │   ├── repository/
    │   │   │   ├── PlayerStockRepository.java
    │   │   │   ├── PriceHistoryRepository.java
    │   │   │   └── ingestion/
    │   │   │       ├── IngestionAthleteRepository.java
    │   │   │       ├── IngestionEventRepository.java
    │   │   │       └── IngestionPlayerStatsRepository.java
    │   │   │
    │   │   └── exception/
    │   │       ├── StockNotFoundException.java
    │   │       └── PricingCalculationException.java
    │   │
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/
    │           ├── V1__create_player_stock.sql
    │           └── V2__create_price_history.sql
    │
    └── test/
        └── java/com/yourapp/stockmarket/
            ├── service/
            │   ├── PricingServiceTest.java
            │   └── PriceRecalculationServiceTest.java
            └── controller/
                └── StockControllerTest.java
```

## Service Class Responsibilities

| Class                       | Responsibility                                                                                                                                                                                              |
| --------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `StockService`              | Stock lookups by ID, ESPN ID, list with filters/pagination. Reads from `PlayerStockRepository`.                                                                                                             |
| `PricingService`            | Pure calculation — no DB access. Takes a stat map and returns fantasy points / a price. Holds the PPR multiplier map and the `parseStat` helper.                                                            |
| `PriceRecalculationService` | Orchestrates a full recalculation run. Reads from `IngestionDataService`, calls `PricingService` for each player, batch-updates `player_stock` rows, writes `price_history` rows.                           |
| `AthleteStockSyncService`   | Reads athletes from `ingestion.athletes` (and team from `ingestion.team_roster_entries`), creates new `player_stock` rows with base prices, updates `full_name`/`position`/`team_espn_id` on existing rows. |
| `IngestionDataService`      | Encapsulates all reads from `ingestion` schema tables. Uses the read-only repos in `repository/ingestion/`. Single class to change if the ingestion service moves to a separate DB or HTTP API.             |

## Controller → Endpoint Mapping

| Controller               | Endpoints                                                                                         |
| ------------------------ | ------------------------------------------------------------------------------------------------- |
| `StockController`        | `GET /stocks`, `GET /stocks/{stockId}`, `GET /stocks/athlete/{athleteEspnId}`                     |
| `PricingAdminController` | `POST /stocks/recalculate`, `POST /stocks/sync-athletes`                                          |
| `GlobalExceptionHandler` | Maps `StockNotFoundException` → 404, `PricingCalculationException` → 500, validation errors → 400 |

---

## Configuration

```yaml
spring:
  application:
    name: stock-market-service
  datasource:
    url: jdbc:postgresql://localhost:5432/sportstock
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: market
        jdbc:
          time_zone: UTC
  flyway:
    enabled: true
    schemas: market
    default-schema: market
    locations: classpath:db/migration

server:
  port: 8082

pricing:
  smoothing-alpha: 0.6
  price-floor: 1.00
  base-prices:
    QB: 15.00
    RB: 12.00
    WR: 10.00
    TE: 8.00
    K: 5.00
```

`PricingConfig.java` maps the `pricing.*` properties using `@ConfigurationProperties(prefix = "pricing")`.

---

## Maven Dependencies (pom.xml)

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.1</version>
</parent>

<properties>
    <java.version>21</java.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Match the Spring Boot version to whatever the ingestion service uses.

---

## Future Features

Not needed for the base version. To be added later.

### Endpoints

| Endpoint                                                             | Purpose                                         |
| -------------------------------------------------------------------- | ----------------------------------------------- |
| `GET /api/v1/stocks/batch?ids=&espnIds=`                             | Bulk lookup (max 100) for portfolio service     |
| `GET /api/v1/stocks/{stockId}/price-history?seasonYear=&seasonType=` | Full season price history for frontend graphing |


### Query enhancements for `GET /api/v1/stocks`

| Param        | Description                        |
| ------------ | ---------------------------------- |
| `teamEspnId` | Filter by team                     |
| `search`     | Player name search (ILIKE)         |
| `sortBy`     | `currentPrice`, `name`, `position` |
| `sortDir`    | `asc` / `desc`                     |

### Price history enhancements

- **Upsert logic:** `ON CONFLICT (player_stock_id, season_year, season_type, week) DO UPDATE` when prices update more than once per week
- **Weekly high/low columns:** Add `high_price` / `low_price` to `price_history` for intra-week updates

### Pricing enhancements

- **Bye week regression:** Price regresses toward position base using `(ALPHA × BASE_PRICE) + ((1-ALPHA) × current_price)`
- **Injury adjustments:** Modify price by status (QUESTIONABLE: -5%, DOUBTFUL: -15%, OUT: -25%, IR: -40%)
- **Configurable weights:** DB-driven `pricing_weight_config` table with admin CRUD endpoints
- **Market-driven pricing:** Layer supply/demand from trades on top of formula

### Files to add

| File                        | Purpose                                           |
| --------------------------- | ------------------------------------------------- |
| `PriceHistoryService.java`  | Read/write price history, season high/low queries |
| `PriceHistoryResponse.java` | DTO for price history endpoint                    |
| `MoversResponse.java`       | DTO for movers endpoint                           |