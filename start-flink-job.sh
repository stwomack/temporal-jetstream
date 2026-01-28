#!/bin/bash
# Start the Flink Enrichment Job (local/embedded mode)
#
# Prerequisites:
#   - Kafka running on localhost:9092
#   - Topics: raw-flight-events (input), flight-events (output)
#
# Data flow:
#   raw-flight-events -> Flink Enrichment -> flight-events -> Spring App

set -e

echo "Starting Flink Enrichment Job..."
echo "  Input topic:  raw-flight-events"
echo "  Output topic: flight-events"
echo ""

./mvnw -q compile exec:java -Dexec.mainClass="com.temporal.jetstream.flink.FlinkEnrichmentJob"
