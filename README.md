# Sport Stock Game — Backend

Multi-service Spring Boot monorepo. Services share a single PostgreSQL database with separate schemas.

## Services

| Service        | Schema      | Port |
| -------------- | ----------- | ---- |
| `ingestion`    | `ingestion` | 8081 |
| `stock-market` | `market`    | 8082 |

## Prerequisites

- Java 21
- Maven 3.9+
- Docker

## Setup

Copy the example env file and fill in any values you want to override (defaults work for local dev):

```bash
cp .env.example .env
```

`.env` values:

| Key           | Default    | Description                      |
| ------------- | ---------- | -------------------------------- |
| `DB_PASSWORD` | `localdev` | Postgres password                |
| `DB_PORT`     | `6432`     | Host port Postgres is exposed on |

## Running locally

**1. start database**
```bash
docker compose -f docker-compose.yml up -d
```

**2. start ingestion service**
```bash
mvn -f services/ingestion/pom.xml spring-boot:run
```

**3. populate ingestion database**
```bash
curl -X POST "http://localhost:8081/api/v1/ingestion/sync/full?seasonYear=2025&seasonType=2&week=1&rosterLimit=500&athletePageSize=250&athletePageCount=10"
```

**4. start stock-market service**
```bash
mvn -f services/stock-market/pom.xml spring-boot:run
```

**5. populate market database**
```bash
curl -X POST "http://localhost:8082/api/v1/stocks/sync-athletes"
```

## Resetting the database

```bash
docker compose -f docker-compose.yml down -v
docker compose -f docker-compose.yml up -d
```


## Accessing the DB from terminal 
```bash
psql -h localhost -p 6432 -U sportstock -d sportstock
```