# Stock Market Service — Context

## Project Overview

This is a Spring Boot microservice that manages NFL player stock prices. It is part of a multi-service monorepo. The ingestion service (already built) pulls NFL data from ESPN into a shared Postgres database and exposes it via REST endpoints. This service calls those endpoints to read NFL data and calculates player stock prices.

## Tech Stack

- Java 21, Spring Boot 4.0.3, Maven
- PostgreSQL (owns the `market` schema only)
- Flyway for migrations
- Spring Data JPA / Hibernate
- RestClient (from `spring-boot-starter-webmvc`) for calling the ingestion API
- Lombok

## Schemas

- `market` — owned by this service (player_stock, price_history)
- Schema creation is handled by `infra/db/init/01-create-schemas.sql`, not by Flyway migrations

## Build & Run

```bash
# Database
docker compose down -v && docker compose up --build -d

# Build
./mvnw -pl services/stock-market clean package

# Run
./mvnw -pl services/stock-market spring-boot:run

# Tests
./mvnw -pl services/stock-market test
```

Ingestion service runs on port `8081`. Stock market service runs on port `8082`.

## Conventions & Rules

- **Timestamps:** All `TIMESTAMP` columns use `TIMESTAMPTZ` in Postgres. Java entities use `Instant` — never `LocalDateTime`.
- **Money:** `DECIMAL(10,2)` in Postgres, `BigDecimal` in Java — never `float` or `double`.
- **Lombok:** Used across all services. Entities use `@Getter`, `@Setter`, `@NoArgsConstructor`. Read-only DTOs use `@Getter`, `@NoArgsConstructor`.
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

## Ingestion Service (REST API)

This service calls the ingestion API over HTTP using RestClient. No direct database access to ingestion tables.

Base URL: `http://localhost:8081/api/v1/ingestion` (configurable via `ingestion.api.base-url`)

No authentication required (for now).

### Endpoints used

| Endpoint                                          | Description                                        | Used by                   |
| ------------------------------------------------- | -------------------------------------------------- | ------------------------- |
| `GET /athletes`                                   | List athletes. Optional `?positionAbbreviation=QB` | AthleteStockSyncService   |
| `GET /athletes/{athleteEspnId}`                   | Single athlete                                     | AthleteStockSyncService   |
| `GET /teams`                                      | List all teams                                     | AthleteStockSyncService   |
| `GET /events?seasonYear=&seasonType=&weekNumber=` | Events for a season/week                           | PriceRecalculationService |
| `GET /events/{eventEspnId}/player-stats`          | Player game stats for an event                     | PriceRecalculationService |

### Ingestion API response shapes (from actual response DTOs)

These are Java records in the ingestion service. Jackson serializes field names as-is (camelCase).

**AthleteResponse:**
```json
{
  "espnId": "3139477",
  "firstName": "Patrick",
  "lastName": "Mahomes",
  "fullName": "Patrick Mahomes II",
  "displayName": "Patrick Mahomes II",
  "positionAbbreviation": "QB",
  "statusName": "Active",
  "statusType": "active"
}
```

**TeamResponse:**
```json
{
  "espnId": "12",
  "abbreviation": "KC",
  "displayName": "Kansas City Chiefs",
  "shortDisplayName": "Chiefs"
}
```

**EventResponse:**
```json
{
  "espnId": "401547417",
  "seasonYear": 2025,
  "seasonType": 2,
  "weekNumber": 6,
  "statusCompleted": true,
  "statusState": "post"
}
```

**PlayerGameStatResponse (one entry per stat category per player):**
```json
{
  "eventEspnId": "401547417",
  "athleteEspnId": "3139477",
  "teamEspnId": "12",
  "statCategory": "passing",
  "stats": { "completions": "22", "passingYards": "301", "passingTouchdowns": "3" }
}
```

A player may have multiple entries per game (one per `statCategory`: passing, rushing, receiving, etc.). These must be merged before calculating fantasy points.

### Stats field

Stats are keyed by stat name with string values. The exact stat keys must be confirmed by calling `GET /events/{eventEspnId}/player-stats` for a completed game and inspecting the response.

---

## Pricing

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

**These keys are best guesses. Verify against actual ingestion API responses before finalizing.**

### Formula

```
fantasy_points = Σ(stat_value × ppr_multiplier)
smoothed_price = (ALPHA × fantasy_points) + ((1 - ALPHA) × current_price)
final_price    = max(PRICE_FLOOR, smoothed_price)
```

### Recalculation flow

1. Call `GET /events?seasonYear=&seasonType=&weekNumber=` — filter to completed events (`statusCompleted == true`)
2. For each completed event, call `GET /events/{eventEspnId}/player-stats`
3. Group/merge stats by `athleteEspnId` (a player may have multiple `statCategory` entries)
4. For each active `PlayerStock`:
   - Look up merged stats for this week (empty if bye/inactive)
   - Calculate fantasy points using PPR multipliers
   - Smooth against current price
   - Enforce price floor
   - Update `market.player_stock.current_price` and `price_updated_at`
   - Insert `market.price_history` row
5. If no completed events found, return `stocksUpdated: 0` — do not fail

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
    │   ├── java/com/sportstock/stockmarket/
    │   │   ├── StockMarketApplication.java
    │   │   │
    │   │   ├── config/
    │   │   │   ├── PricingConfig.java
    │   │   │   └── RestClientConfig.java
    │   │   │
    │   │   ├── client/
    │   │   │   └── IngestionApiClient.java
    │   │   │
    │   │   ├── client/dto/
    │   │   │   ├── IngestionAthleteDto.java
    │   │   │   ├── IngestionTeamDto.java
    │   │   │   ├── IngestionEventDto.java
    │   │   │   └── IngestionPlayerGameStatsDto.java
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
    │   │   │   └── AthleteStockSyncService.java
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
    │   │   │   └── PriceHistoryRepository.java
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
        └── java/com/sportstock/stockmarket/
            ├── service/
            │   ├── PricingServiceTest.java
            │   └── PriceRecalculationServiceTest.java
            └── controller/
                └── StockControllerTest.java
```

## Service / Client Class Responsibilities

| Class                       | Responsibility                                                                                                                                                            |
| --------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `IngestionApiClient`        | HTTP calls to the ingestion API via RestClient. One method per ingestion endpoint. Returns DTOs. Only class that knows about the ingestion API.                           |
| `StockService`              | Stock lookups by ID, ESPN ID, list with filters/pagination. Reads from `PlayerStockRepository`.                                                                           |
| `PricingService`            | Pure calculation — no DB, no HTTP. Takes a stat map and returns fantasy points / a price.                                                                                 |
| `PriceRecalculationService` | Orchestrates a full recalculation. Calls `IngestionApiClient` for events + stats, calls `PricingService` for each player, updates `player_stock`, writes `price_history`. |
| `AthleteStockSyncService`   | Calls `IngestionApiClient` for athletes, creates new `player_stock` rows with base prices, updates metadata on existing rows.                                             |

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
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:6432}/${DB_NAME:sportstock}?currentSchema=market
    username: ${DB_USER:sportstock}
    password: ${DB_PASSWORD:localdev}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: market
        jdbc:
          time_zone: UTC
    open-in-view: false
  flyway:
    enabled: true
    schemas: market
    default-schema: market
    locations: classpath:db/migration
  config:
    import: optional:file:.env[.properties],optional:file:../../.env[.properties]

server:
  port: ${STOCK_MARKET_SERVICE_PORT:8082}

ingestion:
  api:
    base-url: http://localhost:${INGESTION_SERVICE_PORT:8081}/api/v1/ingestion

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

---

## Maven Dependencies (pom.xml)

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.3</version>
    <relativePath/>
</parent>

<groupId>com.sportstock</groupId>
<artifactId>stock-market</artifactId>
<version>0.0.1-SNAPSHOT</version>

<properties>
    <java.version>21</java.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-flyway</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Match the ingestion service's pom structure. Include the Lombok annotation processor and boot plugin excludes (copy from ingestion's pom.xml build section).

---

## Future Features

Not needed for the base version. To be added later.

### Endpoints

| Endpoint                                                             | Purpose                                                      |
| -------------------------------------------------------------------- | ------------------------------------------------------------ |
| `GET /api/v1/stocks/batch?ids=&espnIds=`                             | Bulk lookup (max 100) for portfolio service                  |
| `GET /api/v1/stocks/{stockId}/price-history?seasonYear=&seasonType=` | Full season price history for frontend graphing              |
| `GET /api/v1/stocks/movers?limit=&position=`                         | Top gainers/losers computed from recent `price_history` rows |

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