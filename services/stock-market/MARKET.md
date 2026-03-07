# Stock Market Service — Context

## Project Overview

This is a Spring Boot microservice that manages NFL player stock prices. It is part of a multi-service monorepo. The ingestion service (already built) pulls NFL data from ESPN into a shared Postgres database and exposes it via REST. This service calls the ingestion REST API to get NFL data, then calculates and manages player stock prices.

## Tech Stack

- Java 21, Spring Boot, Maven
- PostgreSQL (owns the `market` schema only)
- Flyway for migrations
- Spring Data JPA / Hibernate
- WebClient for HTTP calls to ingestion service
- Lombok

## Schemas

- `market` — owned by this service (player_stock, price_history)
- Schema creation is handled by `infra/01-create-schemas.sql`, not by Flyway migrations

## Build & Run

```bash
# Database
docker compose -f infra/docker-compose.yml down -v && docker compose -f infra/docker-compose.yml up --build -d

# Build
./mvnw -pl services/stock-market clean package

# Run (ingestion service must be running on port 8081)
./mvnw -pl services/stock-market spring-boot:run

# Tests
./mvnw -pl services/stock-market test
```

Ingestion service: port `8081`. Stock market service: port `8082`.

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

## Ingestion Service (REST API)

This service calls the ingestion REST API (running on port 8081) to get NFL data. No direct database reads from the ingestion schema.

### Endpoints used

| Endpoint                                                              | Description                                        | Used by                                       |
| --------------------------------------------------------------------- | -------------------------------------------------- | --------------------------------------------- |
| `GET /api/v1/ingestion/athletes`                                      | List athletes. Optional `?positionAbbreviation=QB` | AthleteStockSyncService                       |
| `GET /api/v1/ingestion/athletes/{athleteEspnId}`                      | Single athlete                                     | AthleteStockSyncService                       |
| `GET /api/v1/ingestion/teams`                                         | List all teams                                     | AthleteStockSyncService (team ESPN ID lookup) |
| `GET /api/v1/ingestion/teams/{teamEspnId}`                            | Single team                                        | AthleteStockSyncService                       |
| `GET /api/v1/ingestion/events?seasonYear=&weekNumber=`                | Events for a season/week                           | PriceRecalculationService                     |
| `GET /api/v1/ingestion/events/{eventEspnId}/player-stats?teamEspnId=` | Player game stats                                  | PriceRecalculationService                     |

### Response shapes (only fields this service maps)

These DTOs only include the fields this service needs. Jackson's `@JsonIgnoreProperties(ignoreUnknown = true)` handles extra fields in the response.

**Athlete:**
```json
{ "espnId": "3139477", "fullName": "Patrick Mahomes", "positionAbbreviation": "QB" }
```

**Team:**
```json
{ "espnId": "12", "displayName": "Kansas City Chiefs", "abbreviation": "KC" }
```

**Event:**
```json
{ "espnId": "401547417", "seasonYear": 2025, "seasonType": 2, "weekNumber": 6, "statusCompleted": true }
```

**Player game stats (one entry per stat category — a player may have multiple):**
```json
{ "athleteEspnId": "3139477", "statCategory": "passing", "stats": {"passingYards":"301","touchdowns":"3"} }
```

The stats field is a JSON object with string values. The exact stat keys must be confirmed by calling the API and inspecting real data. A player may have multiple entries per game (one per stat category: passing, rushing, receiving, etc.) — these must be merged before calculating fantasy points.

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

1. Call `GET /events?seasonYear=&weekNumber=` — filter for completed events
2. For each completed event, call `GET /events/{espnId}/player-stats`
3. Group/merge stat entries by `athleteEspnId` (a player may have multiple stat categories)
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
    │   ├── java/com/yourapp/stockmarket/
    │   │   ├── StockMarketApplication.java
    │   │   │
    │   │   ├── config/
    │   │   │   ├── PricingConfig.java
    │   │   │   └── WebClientConfig.java
    │   │   │
    │   │   ├── client/
    │   │   │   ├── IngestionApiClient.java
    │   │   │   └── dto/
    │   │   │       ├── IngestionAthleteDto.java
    │   │   │       ├── IngestionTeamDto.java
    │   │   │       ├── IngestionEventDto.java
    │   │   │       └── IngestionPlayerGameStatsDto.java
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
        └── java/com/yourapp/stockmarket/
            ├── service/
            │   ├── PricingServiceTest.java
            │   └── PriceRecalculationServiceTest.java
            └── controller/
                └── StockControllerTest.java
```

## Service Class Responsibilities

| Class                       | Responsibility                                                                                                                     |
| --------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `IngestionApiClient`        | All HTTP calls to the ingestion service. Encapsulates WebClient usage. Returns DTOs.                                               |
| `StockService`              | Stock lookups by ID, ESPN ID, list with filters/pagination.                                                                        |
| `PricingService`            | Pure calculation — no DB, no HTTP. Takes a stat map and returns fantasy points / a price.                                          |
| `PriceRecalculationService` | Orchestrates a recalculation run. Calls `IngestionApiClient` for events/stats, calls `PricingService` for each player, updates DB. |
| `AthleteStockSyncService`   | Calls `IngestionApiClient` for athletes, creates/updates `player_stock` rows.                                                      |

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
    username: ${DB_USERNAME:sportstock}
    password: ${DB_PASSWORD:sportstock}
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

ingestion:
  base-url: http://localhost:8081/api/v1/ingestion

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
        <artifactId>spring-boot-starter-webflux</artifactId>
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
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

`spring-boot-starter-webflux` provides WebClient. Match the Spring Boot version to the ingestion service.

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

- **Upsert logic:** `ON CONFLICT` when prices update more than once per week
- **Weekly high/low columns:** `high_price` / `low_price` for intra-week updates

### Pricing enhancements

- **Bye week regression:** Price regresses toward position base
- **Injury adjustments:** Modify price by injury status
- **Configurable weights:** DB-driven weight table with admin CRUD
- **Market-driven pricing:** Layer supply/demand from trades on top of formula