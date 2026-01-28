#!/bin/bash
# Chaos Flight Launcher - Start 20 flights simultaneously for chaos testing
#
# Usage: ./chaos-flights.sh [base_url]
#   base_url: Optional, defaults to http://localhost:8082
#
# This script launches 20 flights at once to test Temporal's durability
# while you kill services (MongoDB, Kafka, workers, etc.)

set -e

BASE_URL="${1:-http://localhost:8082}"
API_URL="$BASE_URL/api/flights/start"

# Arrays of airports for variety
DEPARTURES=("ORD" "DFW" "LAX" "JFK" "ATL" "DEN" "SFO" "SEA" "MIA" "BOS")
ARRIVALS=("PHX" "LAS" "MSP" "DTW" "PHL" "CLT" "IAH" "EWR" "SAN" "TPA")
GATES=("A1" "A2" "B3" "B4" "C5" "C6" "D7" "D8" "E9" "E10" "F11" "F12" "G13" "G14" "H15" "H16" "J17" "J18" "K19" "K20")

# Get today's date
TODAY=$(date +%Y-%m-%d)
NOW=$(date -u +%Y-%m-%dT%H:%M:%SZ)

echo "=============================================="
echo "  Chaos Flight Launcher"
echo "=============================================="
echo "Target: $API_URL"
echo "Date: $TODAY"
echo ""
echo "Launching 20 flights..."
echo ""

# Track results
SUCCESS=0
FAILED=0

# Launch 20 flights
for i in $(seq 1 2000); do
    # Generate flight data
    FLIGHT_NUM=$(printf "CHS%03d" $i)
    DEP_IDX=$(( (i - 1) % 10 ))
    ARR_IDX=$(( (i + 4) % 10 ))

    DEP="${DEPARTURES[$DEP_IDX]}"
    ARR="${ARRIVALS[$ARR_IDX]}"
    GATE="${GATES[$((i - 1))]}"
    AIRCRAFT=$(printf "N%03dCH" $i)

    # Create JSON payload
    JSON=$(cat <<EOF
{
    "flightNumber": "$FLIGHT_NUM",
    "flightDate": "$TODAY",
    "departureStation": "$DEP",
    "arrivalStation": "$ARR",
    "scheduledDeparture": "$NOW",
    "scheduledArrival": "$NOW",
    "gate": "$GATE",
    "aircraft": "$AIRCRAFT"
}
EOF
)

    # Launch flight (background, non-blocking)
    (
        RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL" \
            -H "Content-Type: application/json" \
            -d "$JSON" 2>/dev/null)

        HTTP_CODE=$(echo "$RESPONSE" | tail -n1)

        if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
            echo "  [OK]  $FLIGHT_NUM: $DEP -> $ARR (Gate $GATE)"
        else
            echo "  [ERR] $FLIGHT_NUM: HTTP $HTTP_CODE"
        fi
    ) &
done

# Wait for all background jobs to complete
wait

echo ""
echo "=============================================="
echo "  All 20 flights launched!"
echo "=============================================="
echo ""
echo "Now go cause chaos:"
echo "  - Kill MongoDB:  brew services stop mongodb-community"
echo "  - Kill Kafka:    brew services stop kafka"
echo "  - Kill Worker:   Use 'Simulate Failure' button in UI"
echo "  - Kill App:      Ctrl+C the Spring Boot process"
echo ""
echo "Watch the Temporal UI at http://localhost:8233"
echo "Workflows will resume when services come back!"
