#!/bin/bash

# Move to the directory where the script is located
cd "$(dirname "$0")"

echo "Initializing Banking Microservices Infrastructure..."

# Stop and remove volumes
docker compose down -v

# Start infrastructure
docker compose up -d

echo "Infrastructure started. Waiting for initialization scripts to finish..."
sleep 10

docker ps

echo "----------------------------------------------------------"
echo "Initialization Check:"
echo "1. Kafka Topics: Run 'docker logs kafka-setup'"
echo "2. Postgres: Run 'docker exec -it postgres psql -U user -d banking -c \"\dn\"'"
echo "----------------------------------------------------------"
