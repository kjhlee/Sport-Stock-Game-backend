#!/usr/bin/env bash

# You have to chmod +x this file 
echo "Starting SportStock services..."

java -jar services/ingestion/target/ingestion-0.0.1-SNAPSHOT.jar &
java -jar services/user-authentication/target/user-authentication-0.0.1.jar &
java -jar services/league/target/league-0.0.1-SNAPSHOT.jar &
java -jar services/transaction/target/transaction-0.0.1-SNAPSHOT.jar &

