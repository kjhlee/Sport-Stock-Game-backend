# Service Port Registry

| Service     | Port | Env Var                    | Notes                              |
|-------------|------|----------------------------|------------------------------------|
| Postgres    | 6432 | `DB_PORT`                  | Host-side; maps to container 5432  |
| Ingestion   | 8090 | `INGESTION_SERVICE_PORT`   | Spring Boot HTTP                   |
| League      | 8100 | `LEAGUE_SERVICE_PORT`      | Spring Boot HTTP                   |
| Transaction | 8110 | `TRANSACTION_SERVICE_PORT` | Spring Boot HTTP                   |