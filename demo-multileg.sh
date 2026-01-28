#!/bin/bash
# Multi-Leg Flight Demo - Demonstrates parent-child workflow orchestration
#
# Usage: ./demo-multileg.sh [base_url]
#   base_url: Optional, defaults to http://localhost:8082
#
# This script creates a 3-leg journey: ORD -> DFW -> PHX -> LAX
# showing how Temporal orchestrates connecting flights with:
#   - Sequential execution (leg 2 waits for leg 1)
#   - Cascade cancellation (cancel one leg, rest are cancelled)
#   - Aircraft handoff between legs

set -e

BASE_URL="${1:-http://localhost:8082}"
API_URL="$BASE_URL/api/flights/journey"

# Get today's date
TODAY=$(date +%Y-%m-%d)
NOW=$(date -u +%Y-%m-%dT%H:%M:%SZ)

# Generate unique journey ID
JOURNEY_ID="JRN$(date +%H%M%S)"

echo "=============================================="
echo "  Multi-Leg Flight Demo"
echo "=============================================="
echo ""
echo "Journey ID: $JOURNEY_ID"
echo "Route: ORD -> DFW -> PHX -> LAX (3 legs)"
echo ""
echo "This demonstrates:"
echo "  1. Parent workflow (journey) orchestrating child workflows (legs)"
echo "  2. Sequential execution - each leg waits for previous to complete"
echo "  3. Cascade cancellation - cancel one leg, rest auto-cancel"
echo "  4. Aircraft handoff between legs"
echo ""
echo "=============================================="
echo ""

# Create the multi-leg journey payload
JSON=$(cat <<EOF
{
    "journeyId": "$JOURNEY_ID",
    "flights": [
        {
            "flightNumber": "${JOURNEY_ID}-L1",
            "flightDate": "$TODAY",
            "departureStation": "ORD",
            "arrivalStation": "DFW",
            "scheduledDeparture": "$NOW",
            "scheduledArrival": "$NOW",
            "gate": "B12",
            "aircraft": "N100ML"
        },
        {
            "flightNumber": "${JOURNEY_ID}-L2",
            "flightDate": "$TODAY",
            "departureStation": "DFW",
            "arrivalStation": "PHX",
            "scheduledDeparture": "$NOW",
            "scheduledArrival": "$NOW",
            "gate": "C22",
            "aircraft": "N100ML"
        },
        {
            "flightNumber": "${JOURNEY_ID}-L3",
            "flightDate": "$TODAY",
            "departureStation": "PHX",
            "arrivalStation": "LAX",
            "scheduledDeparture": "$NOW",
            "scheduledArrival": "$NOW",
            "gate": "A5",
            "aircraft": "N100ML"
        }
    ]
}
EOF
)

echo "Starting journey..."
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_URL" \
    -H "Content-Type: application/json" \
    -d "$JSON")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
    echo "[SUCCESS] Journey started!"
    echo ""
    echo "Response:"
    echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
    echo ""
    echo "=============================================="
    echo "  What to watch for:"
    echo "=============================================="
    echo ""
    echo "1. Temporal UI (http://localhost:8233):"
    echo "   - Parent workflow: journey-$JOURNEY_ID"
    echo "   - Child workflows: flight-${JOURNEY_ID}-L1-*, flight-${JOURNEY_ID}-L2-*, flight-${JOURNEY_ID}-L3-*"
    echo ""
    echo "2. App UI (http://localhost:8082):"
    echo "   - Watch legs appear one at a time as each completes"
    echo "   - Leg 2 won't start until Leg 1 reaches COMPLETED"
    echo ""
    echo "3. Try cancelling mid-journey:"
    echo "   curl -X POST '$BASE_URL/api/flights/${JOURNEY_ID}-L2/cancel' \\"
    echo "     -H 'Content-Type: application/json' \\"
    echo "     -d '{\"reason\": \"Testing cascade cancellation\"}'"
    echo ""
    echo "   This will cancel Leg 2 AND automatically cancel Leg 3!"
    echo ""
    echo "=============================================="
    echo "  Timeline (20s per phase × 5 phases = ~100s per leg)"
    echo "=============================================="
    echo ""
    echo "  Leg 1 (ORD->DFW): ~100 seconds"
    echo "  Turnaround:       ~1 second"
    echo "  Leg 2 (DFW->PHX): ~100 seconds"
    echo "  Turnaround:       ~1 second"
    echo "  Leg 3 (PHX->LAX): ~100 seconds"
    echo "  ─────────────────────────────"
    echo "  Total:            ~5 minutes"
    echo ""
else
    echo "[ERROR] Failed to start journey"
    echo "HTTP Status: $HTTP_CODE"
    echo "Response: $BODY"
    exit 1
fi
