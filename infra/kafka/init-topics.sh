#!/bin/bash
export PATH=$PATH:/opt/kafka/bin
# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."

# Function to check if Kafka is ready
wait_for_kafka() {
  local retries=0
  until kafka-topics.sh --bootstrap-server kafka:29092 --list > /dev/null 2>&1; do
    echo "Kafka not ready yet..."
    sleep 2
    ((retries++))
    if [ $retries -gt 30 ]; then
      echo "Kafka initialization failed (timeout)."
      exit 1
    fi
  done
  echo "Kafka is ready!"
}

wait_for_kafka

# Create topics
echo "Creating topics..."
TOPICS=(
  "transactions.commands"
  "transactions.commands.DLT"
  "accounts.events"
  "accounts.events.DLT"
  "transactions.events"
  "transactions.events.DLT"
  "notifications.events"
  "notifications.events.DLT"
  "customers.events.created"
)

for topic in "${TOPICS[@]}"; do
  kafka-topics.sh --bootstrap-server kafka:29092 --create --if-not-exists --topic "$topic" --partitions 3 --replication-factor 1
  echo "Created topic: $topic"
done

echo "All topics created successfully."
