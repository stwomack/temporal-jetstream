#!/bin/bash
# Chaos Flight Launcher - Start multiple flights simultaneously for chaos testing
#
# Usage: ./demo-chaos.sh [count] [base_url]
#   count:    Number of flights to launch (default: 20)
#   base_url: Optional, defaults to http://localhost:8082
#
# Examples:
#   ./demo-chaos.sh              # Launch 20 flights
#   ./demo-chaos.sh 50           # Launch 50 flights
#   ./demo-chaos.sh 200          # Launch 200 flights
#   ./demo-chaos.sh 100 http://host:8082  # Launch 100 flights to custom URL
#
# Flight numbers use format: <BatchID><SeqNum> (e.g., X7A001, X7A002)
# BatchID is unique per run, so you can run the script multiple times.

set -e

# Parse arguments
COUNT="${1:-20}"
BASE_URL="${2:-http://localhost:8082}"
API_URL="$BASE_URL/api/flights/start"

# Generate unique batch ID (3 chars: letter + digit + letter)
BATCH_ID=$(cat /dev/urandom | LC_ALL=C tr -dc 'A-Z' | head -c1)$(cat /dev/urandom | LC_ALL=C tr -dc '0-9' | head -c1)$(cat /dev/urandom | LC_ALL=C tr -dc 'A-Z' | head -c1)

# Arrays of airports for variety
DEPARTURES=("ORD" "DFW" "LAX" "JFK" "ATL" "DEN" "SFO" "SEA" "MIA" "BOS")
ARRIVALS=("PHX" "LAS" "MSP" "DTW" "PHL" "CLT" "IAH" "EWR" "SAN" "TPA")

# Get today's date
TODAY=$(date +%Y-%m-%d)
NOW=$(date -u +%Y-%m-%dT%H:%M:%SZ)

echo "=============================================="
echo "  Chaos Flight Launcher"
echo "=============================================="
echo "Target: $API_URL"
echo "Batch:  $BATCH_ID"
echo "Count:  $COUNT flights"
echo ""
echo "Launching flights..."
echo ""

# Launch flights
for i in $(seq 1 $COUNT); do
    # Generate flight data with batch ID prefix
    FLIGHT_NUM=$(printf "%s%03d" "$BATCH_ID" $i)
    DEP_IDX=$(( (i - 1) % 10 ))
    ARR_IDX=$(( (i + 4) % 10 ))
    GATE_NUM=$(( (i - 1) % 20 + 1 ))

    DEP="${DEPARTURES[$DEP_IDX]}"
    ARR="${ARRIVALS[$ARR_IDX]}"
    GATE=$(printf "%c%d" $(printf "\\$(printf '%03o' $((65 + (i - 1) % 10)))") $GATE_NUM)
    AIRCRAFT=$(printf "N%s%03d" "$BATCH_ID" $i)

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
echo "  All $COUNT flights launched! (Batch: $BATCH_ID)"
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
