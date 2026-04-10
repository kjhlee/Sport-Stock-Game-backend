# Service Port Registry

| Service      | Port | Env Var                     | Notes                             |
|--------------|------|-----------------------------|-----------------------------------|
| Postgres     | 6432 | `DB_PORT`                   | Host-side; maps to container 5432 |
| Ingestion    | 8090 | `INGESTION_SERVICE_PORT`    | Spring Boot HTTP                  |
| League       | 8100 | `LEAGUE_SERVICE_PORT`       | Spring Boot HTTP                  |
| Transaction  | 8110 | `TRANSACTION_SERVICE_PORT`  | Spring Boot HTTP                  |
| User Auth    | 8120 | `USER_AUTH_PORT`            | Spring Boot HTTP                  |
| Stock Market | 8130 | `STOCK_MARKET_SERVICE_PORT` | Spring Boot HTTP                  |
| Portfolio    | 8140 | `PORTFOLIO_PORT`            | Spring Boot HTTP                  |
| Scheduler    | 8150 | `SCHEDULER_SERVICE_PORT`    | Spring Boot HTTP                  |
