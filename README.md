# Sport-Stock-Game-backend

## Notes:
- I setup the db to create 3 schemas on init (market, users, ingestion). Just set it up in app properties for other servicess
- 

## Nuke DB/ build from root
- docker compose -f infra/docker-compose.yml down -v 
- docker compose -f infra/docker-compose.yml up --build -d