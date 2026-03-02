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

> Do not set `SERVER_PORT` in `.env` — each service has its own default port.

## Running locally

Each step runs in a separate terminal.

**1. Start the database**

```bash
docker compose -f infra/docker-compose.yml up -d
```

This starts a single Postgres instance on port `6432`. Both the `ingestion` and `market` schemas are created automatically on first run.

**2. Start the ingestion service**

```bash
mvn -f services/ingestion/pom.xml spring-boot:run
```

**3. Start the stock-market service**

```bash
mvn -f services/stock-market/pom.xml spring-boot:run
```

## Resetting the database

```bash
docker compose -f infra/docker-compose.yml down -v
docker compose -f infra/docker-compose.yml up -d
```

The `-v` flag drops the volume, giving you a clean database. Flyway migrations re-run on the next service startup.
