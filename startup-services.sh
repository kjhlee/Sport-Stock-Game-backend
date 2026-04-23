#!/usr/bin/env bash

# You have to chmod +x this file 
echo "Starting SportStock services..."

java -jar services/ingestion/target/ingestion-0.0.1-SNAPSHOT.jar &
java -jar services/user-authentication/target/user-authentication-0.0.1-SNAPSHOT.jar &
java -jar services/league/target/league-0.0.1-SNAPSHOT.jar &
java -jar services/transaction/target/transaction-0.0.1-SNAPSHOT.jar &
java -jar services/stock-market/target/stock-market-0.0.1-SNAPSHOT.jar &
java -jar services/scheduler/target/scheduler-0.0.1-SNAPSHOT.jar &
java -jar services/portfolio/target/portfolio-0.0.1-SNAPSHOT.jar &
