// Global state
let stompClient = null;
let selectedFlight = null;
let activeFlights = new Map();

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    connectWebSocket();
    setupFormHandlers();
    // Refresh flights periodically (fallback if WebSocket misses updates)
    setInterval(refreshFlights, 30000); // Every 30 seconds
});

// WebSocket Connection
function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function(frame) {
        console.log('Connected: ' + frame);
        updateConnectionStatus(true);

        // Subscribe to flight updates
        stompClient.subscribe('/topic/flights', function(message) {
            const flight = JSON.parse(message.body);
            console.log('Received flight update:', flight);
            handleFlightUpdate(flight);
        });

        // Subscribe to flight events
        stompClient.subscribe('/topic/flight-events', function(message) {
            const event = JSON.parse(message.body);
            console.log('Received flight event:', event);
            addEventLog(event.flightNumber, event.state, event.message);
        });
    }, function(error) {
        console.error('WebSocket error:', error);
        updateConnectionStatus(false);
        // Attempt reconnection after 5 seconds
        setTimeout(connectWebSocket, 5000);
    });
}

function updateConnectionStatus(connected) {
    const badge = document.getElementById('connectionStatus');
    if (connected) {
        badge.textContent = 'Connected';
        badge.className = 'badge bg-success';
    } else {
        badge.textContent = 'Disconnected';
        badge.className = 'badge bg-danger';
    }
}

// Handle flight updates from WebSocket
function handleFlightUpdate(flight) {
    activeFlights.set(flight.flightNumber, flight);
    renderFlights();

    // Update selected flight details if this is the selected flight
    if (selectedFlight && selectedFlight.flightNumber === flight.flightNumber) {
        selectedFlight = flight;
        displayFlightDetails(flight);
    }

    addEventLog(flight.flightNumber, flight.currentState, `State: ${flight.currentState}`);
}

// Form handlers
function setupFormHandlers() {
    document.getElementById('startFlightForm').addEventListener('submit', async function(e) {
        e.preventDefault();
        await startFlight();
    });
}

async function startFlight() {
    const flightNumber = document.getElementById('flightNumber').value;
    const departureStation = document.getElementById('departureStation').value;
    const arrivalStation = document.getElementById('arrivalStation').value;
    const gate = document.getElementById('gate').value;
    const aircraft = document.getElementById('aircraft').value;

    const now = new Date();
    const departure = new Date(now.getTime() + 2 * 60 * 60 * 1000); // 2 hours from now
    const arrival = new Date(departure.getTime() + 3 * 60 * 60 * 1000); // 3 hours after departure

    const flightData = {
        flightNumber: flightNumber,
        flightDate: now.toISOString().split('T')[0],
        departureStation: departureStation,
        arrivalStation: arrivalStation,
        scheduledDeparture: departure.toISOString(),
        scheduledArrival: arrival.toISOString(),
        gate: gate,
        aircraft: aircraft
    };

    try {
        const response = await fetch('/api/flights/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(flightData)
        });

        if (response.ok) {
            const result = await response.json();
            console.log('Flight started:', result);
            addEventLog(flightNumber, 'SCHEDULED', 'Flight started successfully');
            document.getElementById('startFlightForm').reset();

            // Fetch the flight details after a short delay to let it initialize
            setTimeout(() => fetchFlightDetails(flightNumber), 1000);
        } else {
            const error = await response.json();
            alert('Error starting flight: ' + error.message);
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Error starting flight');
    }
}

async function fetchFlightDetails(flightNumber) {
    try {
        const response = await fetch(`/api/flights/${flightNumber}/details`);
        if (response.ok) {
            const flight = await response.json();
            activeFlights.set(flightNumber, flight);
            renderFlights();
        }
    } catch (error) {
        console.error('Error fetching flight details:', error);
    }
}

// Refresh all flights (manual refresh)
async function refreshFlights() {
    // Note: In a real app, we'd have a list endpoint
    // For now, we just re-query the flights we know about
    for (const flightNumber of activeFlights.keys()) {
        await fetchFlightDetails(flightNumber);
    }
}

// Render flights list
function renderFlights() {
    const flightsList = document.getElementById('flightsList');

    if (activeFlights.size === 0) {
        flightsList.innerHTML = '<p class="text-muted text-center">No active flights. Start a flight to see it here.</p>';
        return;
    }

    flightsList.innerHTML = '';

    for (const flight of activeFlights.values()) {
        const flightCard = createFlightCard(flight);
        flightsList.appendChild(flightCard);
    }
}

function createFlightCard(flight) {
    const card = document.createElement('div');
    card.className = 'flight-card';
    if (selectedFlight && selectedFlight.flightNumber === flight.flightNumber) {
        card.classList.add('selected');
    }

    card.onclick = () => selectFlight(flight);

    const stateClass = flight.currentState || 'SCHEDULED';
    const delayText = flight.delay > 0 ? `<span class="delay-indicator">+${flight.delay} min delay</span>` : '';

    card.innerHTML = `
        <div class="flight-header">
            <div class="flight-number">${flight.flightNumber}</div>
            <span class="badge state-badge ${stateClass}">${stateClass}</span>
        </div>
        <div class="flight-info">
            <div class="flight-info-item">
                <span class="flight-info-label">Route</span>
                <span class="flight-info-value">${flight.departureStation} → ${flight.arrivalStation}</span>
            </div>
            <div class="flight-info-item">
                <span class="flight-info-label">Gate</span>
                <span class="flight-info-value">${flight.gate || 'N/A'}</span>
            </div>
            <div class="flight-info-item">
                <span class="flight-info-label">Aircraft</span>
                <span class="flight-info-value">${flight.aircraft || 'N/A'}</span>
            </div>
            <div class="flight-info-item">
                <span class="flight-info-label">Delay</span>
                <span class="flight-info-value">${delayText || '0 min'}</span>
            </div>
        </div>
    `;

    return card;
}

function selectFlight(flight) {
    selectedFlight = flight;
    renderFlights(); // Re-render to show selection
    displayFlightDetails(flight);
}

function displayFlightDetails(flight) {
    const card = document.getElementById('flightDetailsCard');
    card.style.display = 'block';

    document.getElementById('selectedFlightNumber').textContent = flight.flightNumber;

    const stateSpan = document.getElementById('detailState');
    stateSpan.textContent = flight.currentState || 'SCHEDULED';
    stateSpan.className = `badge ${flight.currentState || 'SCHEDULED'}`;

    document.getElementById('detailRoute').textContent = `${flight.departureStation} → ${flight.arrivalStation}`;
    document.getElementById('detailGate').textContent = flight.gate || 'N/A';
    document.getElementById('detailAircraft').textContent = flight.aircraft || 'N/A';
    document.getElementById('detailDelay').textContent = flight.delay || '0';
    document.getElementById('detailDeparture').textContent = formatDateTime(flight.scheduledDeparture);
    document.getElementById('detailArrival').textContent = formatDateTime(flight.scheduledArrival);
}

// Signal operations
function showDelayModal() {
    if (!selectedFlight) return;
    const modal = new bootstrap.Modal(document.getElementById('delayModal'));
    modal.show();
}

function showGateChangeModal() {
    if (!selectedFlight) return;
    const modal = new bootstrap.Modal(document.getElementById('gateChangeModal'));
    modal.show();
}

async function announceDelay() {
    if (!selectedFlight) return;

    const minutes = parseInt(document.getElementById('delayMinutes').value);
    if (!minutes || minutes < 0) {
        alert('Please enter a valid delay in minutes');
        return;
    }

    try {
        const response = await fetch(`/api/flights/${selectedFlight.flightNumber}/delay`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ minutes: minutes })
        });

        if (response.ok) {
            console.log('Delay announced');
            bootstrap.Modal.getInstance(document.getElementById('delayModal')).hide();
            document.getElementById('delayMinutes').value = '';
        } else {
            const error = await response.json();
            alert('Error announcing delay: ' + error.message);
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Error announcing delay');
    }
}

async function changeGate() {
    if (!selectedFlight) return;

    const newGate = document.getElementById('newGate').value;
    if (!newGate) {
        alert('Please enter a gate number');
        return;
    }

    try {
        const response = await fetch(`/api/flights/${selectedFlight.flightNumber}/gate`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ newGate: newGate })
        });

        if (response.ok) {
            console.log('Gate changed');
            bootstrap.Modal.getInstance(document.getElementById('gateChangeModal')).hide();
            document.getElementById('newGate').value = '';
        } else {
            const error = await response.json();
            alert('Error changing gate: ' + error.message);
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Error changing gate');
    }
}

async function cancelSelectedFlight() {
    if (!selectedFlight) return;

    if (!confirm(`Are you sure you want to cancel flight ${selectedFlight.flightNumber}?`)) {
        return;
    }

    try {
        const response = await fetch(`/api/flights/${selectedFlight.flightNumber}/cancel`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ reason: 'Cancelled by operator' })
        });

        if (response.ok) {
            console.log('Flight cancelled');
        } else {
            const error = await response.json();
            alert('Error cancelling flight: ' + error.message);
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Error cancelling flight');
    }
}

// Simulate worker failure
async function simulateFailure() {
    if (!confirm('This will restart the Temporal worker to demonstrate failure recovery.\n\nActive workflows will pause and then resume automatically.\n\nContinue?')) {
        return;
    }

    addEventLog('SYSTEM', 'ADMIN', 'Simulating worker failure...');

    try {
        const response = await fetch('/api/admin/restart-worker', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });

        if (response.ok) {
            const result = await response.json();
            addEventLog('SYSTEM', 'ADMIN', result.message);
            alert('Worker restarted successfully! Workflows will continue from their last checkpoint.');
        } else {
            const error = await response.json();
            alert('Error restarting worker: ' + error.message);
            addEventLog('SYSTEM', 'ERROR', 'Failed to restart worker: ' + error.message);
        }
    } catch (error) {
        console.error('Error:', error);
        alert('Error restarting worker: ' + error.message);
        addEventLog('SYSTEM', 'ERROR', 'Failed to restart worker: ' + error.message);
    }
}

// Event log
function addEventLog(flightNumber, state, message) {
    const eventLog = document.getElementById('eventLog');

    // Clear "waiting" message if present
    if (eventLog.querySelector('.text-muted')) {
        eventLog.innerHTML = '';
    }

    const eventItem = document.createElement('div');
    eventItem.className = 'event-item';

    const timestamp = new Date().toLocaleTimeString();
    eventItem.innerHTML = `
        <div class="timestamp">${timestamp}</div>
        <div class="message"><strong>${flightNumber}</strong>: ${message}</div>
    `;

    // Add to top of log
    eventLog.insertBefore(eventItem, eventLog.firstChild);

    // Keep only last 20 events
    while (eventLog.children.length > 20) {
        eventLog.removeChild(eventLog.lastChild);
    }
}

// History / Audit Trail
let currentHistory = null;

async function showHistoryModal() {
    if (!selectedFlight) return;

    const modal = new bootstrap.Modal(document.getElementById('historyModal'));
    document.getElementById('historyFlightNumber').textContent = selectedFlight.flightNumber;

    // Show loading state
    document.getElementById('historyTimeline').innerHTML = `
        <div class="text-center">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
            <p class="mt-2">Loading workflow history...</p>
        </div>
    `;

    modal.show();

    // Fetch history
    try {
        const response = await fetch(`/api/flights/${selectedFlight.flightNumber}/history`);
        if (response.ok) {
            currentHistory = await response.json();
            renderHistoryTimeline(currentHistory);
        } else {
            const error = await response.json();
            document.getElementById('historyTimeline').innerHTML = `
                <div class="alert alert-danger">
                    <strong>Error:</strong> ${error.message || 'Failed to load history'}
                </div>
            `;
        }
    } catch (error) {
        console.error('Error fetching history:', error);
        document.getElementById('historyTimeline').innerHTML = `
            <div class="alert alert-danger">
                <strong>Error:</strong> Failed to load workflow history
            </div>
        `;
    }
}

function renderHistoryTimeline(history) {
    const timeline = document.getElementById('historyTimeline');

    if (!history || history.length === 0) {
        timeline.innerHTML = '<p class="text-muted text-center">No history events found</p>';
        return;
    }

    timeline.innerHTML = '<div class="timeline"></div>';
    const timelineDiv = timeline.querySelector('.timeline');

    for (const event of history) {
        const eventDiv = document.createElement('div');
        eventDiv.className = `timeline-event ${event.category}`;

        eventDiv.innerHTML = `
            <div class="timeline-event-header">
                <span class="timeline-event-id">#${event.eventId}</span>
                <span class="timeline-event-timestamp">${event.timestamp}</span>
            </div>
            <div class="timeline-event-description">${event.description}</div>
            <div class="timeline-event-type">${event.eventType}</div>
        `;

        timelineDiv.appendChild(eventDiv);
    }
}

function exportHistory() {
    if (!currentHistory || !selectedFlight) {
        alert('No history data available to export');
        return;
    }

    const exportData = {
        flightNumber: selectedFlight.flightNumber,
        exportedAt: new Date().toISOString(),
        eventCount: currentHistory.length,
        history: currentHistory
    };

    const dataStr = JSON.stringify(exportData, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });

    const downloadLink = document.createElement('a');
    downloadLink.href = URL.createObjectURL(dataBlob);
    downloadLink.download = `flight-${selectedFlight.flightNumber}-history-${new Date().toISOString().split('T')[0]}.json`;

    document.body.appendChild(downloadLink);
    downloadLink.click();
    document.body.removeChild(downloadLink);

    addEventLog(selectedFlight.flightNumber, 'AUDIT', 'Workflow history exported');
}

// Utility functions
function formatDateTime(isoString) {
    if (!isoString) return 'N/A';
    const date = new Date(isoString);
    return date.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}
