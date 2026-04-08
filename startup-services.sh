#!/usr/bin/env bash

# You have to chmod +x this file 
echo "Starting SportStock services..."

java -jar services/ingestion/target/ingestion-0.0.1-SNAPSHOT.jar > /dev/null 2>&1 &
java -jar services/user-authentication/target/user-authentication-0.0.1-SNAPSHOT.jar > /dev/null 2>&1 &
java -jar services/league/target/league-0.0.1-SNAPSHOT.jar > /dev/null 2>&1 &
java -jar services/transaction/target/transaction-0.0.1-SNAPSHOT.jar > /dev/null 2>&1 &
java -jar services/stock-market/target/stock-market-0.0.1-SNAPSHOT.jar > /dev/null 2>&1 &
