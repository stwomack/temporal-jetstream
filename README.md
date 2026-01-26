# Temporal Jetstream - Airline Flight Lifecycle Orchestration Demo

A demonstration application showing how Temporal provides durability, reliability, scalability, and consistency for multi-day airline flight lifecycle orchestration. Complements existing Kafka/Flink streaming architecture by providing durable execution guarantees for long-running flight processes.

## Prerequisites

Before running the application, ensure you have the following installed:

- **Java 21** - [Download from Oracle](https://www.oracle.com/java/technologies/downloads/#java21) or use SDKMAN
- **Docker & Docker Compose** - For running Kafka and MongoDB
- **Temporal Server** - Running locally via `temporal server start-dev`

### Installing Temporal CLI

```bash
# macOS
brew install temporal

# Linux
curl -sSf https://temporal.download/cli.sh | sh

# Windows (via Scoop)
scoop install temporal
```

## Quick Start

### 1. Start Temporal Server

In a separate terminal, start the Temporal development server:

```bash
temporal server start-dev
```

This will start Temporal on `localhost:7233` with the Web UI available at `http://localhost:8233`.

### 2. Start Supporting Services

Start Kafka and MongoDB using Docker Compose:

```bash
docker-compose up -d
```

Verify services are running:
```bash
docker-compose ps
```

### 3. Build the Application

```bash
./mvnw clean install
```

### 4. Run the Application

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

## Web UI

The application includes an embedded web UI for real-time flight monitoring and control.

### Accessing the UI

Open your browser and navigate to:

```
http://localhost:8080
```

### UI Features

The web interface provides:

1. **Start New Flight** - Form to create new flight workflows with all required details
2. **Active Flights List** - Real-time view of all active flights with current state
3. **Flight Details Panel** - Detailed view of selected flight including:
   - Current state (SCHEDULED, BOARDING, DEPARTED, IN_FLIGHT, LANDED, COMPLETED, CANCELLED)
   - Route information (departure/arrival stations)
   - Gate and aircraft assignments
   - Delay information
   - Scheduled times
4. **Flight Operations** - Interactive buttons to:
   - Announce delays
   - Change gates
   - Cancel flights
5. **Event Log** - Real-time stream of flight events and state changes
6. **WebSocket Connection Status** - Indicator showing real-time connection status

### Real-time Updates

The UI uses WebSocket (STOMP over SockJS) to receive real-time updates:
- Flight state changes are pushed to the UI immediately
- No page refresh needed to see updates
- Event log shows all flight events as they occur
- Connection automatically reconnects if interrupted

### Using the UI

1. **Start a flight**: Fill in the form on the left panel and click "Start Flight"
2. **View flight status**: Flights appear in the Active Flights panel on the right
3. **Select a flight**: Click on any flight card to view detailed information
4. **Perform operations**: Use the buttons in the Flight Details panel to:
   - Click "Announce Delay" to add a delay (in minutes)
   - Click "Change Gate" to update the gate assignment
   - Click "Cancel Flight" to cancel the flight workflow
5. **Monitor events**: Watch the Event Log panel for real-time updates

## REST API Endpoints

The application provides REST endpoints to interact with flight workflows:

### Start a Flight

Start a new flight workflow:

```bash
curl -X POST http://localhost:8080/api/flights/start \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "AA1234",
    "flightDate": "2026-01-26",
    "departureStation": "ORD",
    "arrivalStation": "DFW",
    "scheduledDeparture": "2026-01-26T10:00:00",
    "scheduledArrival": "2026-01-26T12:30:00",
    "gate": "A12",
    "aircraft": "N123AA"
  }'
```

Response:
```json
{
  "workflowId": "flight-AA1234-2026-01-26",
  "flightNumber": "AA1234",
  "message": "Flight workflow started successfully"
}
```

### Announce Delay

Send a delay signal to a running flight:

```bash
curl -X POST http://localhost:8080/api/flights/AA1234/delay?flightDate=2026-01-26 \
  -H "Content-Type: application/json" \
  -d '{"minutes": 45}'
```

Response:
```json
{
  "error": "SUCCESS",
  "message": "Delay of 45 minutes announced"
}
```

### Change Gate

Send a gate change signal to a running flight:

```bash
curl -X POST http://localhost:8080/api/flights/AA1234/gate?flightDate=2026-01-26 \
  -H "Content-Type: application/json" \
  -d '{"newGate": "B24"}'
```

Response:
```json
{
  "error": "SUCCESS",
  "message": "Gate changed to B24"
}
```

### Cancel Flight

Send a cancellation signal to a running flight:

```bash
curl -X POST http://localhost:8080/api/flights/AA1234/cancel?flightDate=2026-01-26 \
  -H "Content-Type: application/json" \
  -d '{"reason": "Weather conditions"}'
```

Response:
```json
{
  "error": "SUCCESS",
  "message": "Flight cancelled: Weather conditions"
}
```

### Query Flight State

Get the current state of a flight:

```bash
curl http://localhost:8080/api/flights/AA1234/state?flightDate=2026-01-26
```

Response:
```json
{
  "flightNumber": "AA1234",
  "currentState": "BOARDING"
}
```

### Query Flight Details

Get complete flight details:

```bash
curl http://localhost:8080/api/flights/AA1234/details?flightDate=2026-01-26
```

Response:
```json
{
  "flightNumber": "AA1234",
  "flightDate": "2026-01-26",
  "departureStation": "ORD",
  "arrivalStation": "DFW",
  "scheduledDeparture": "2026-01-26T10:00:00",
  "scheduledArrival": "2026-01-26T12:30:00",
  "currentState": "BOARDING",
  "gate": "B24",
  "aircraft": "N123AA",
  "delay": 45
}
```

### Error Responses

When a flight is not found:
```json
{
  "error": "WORKFLOW_NOT_FOUND",
  "message": "Flight not found: AA1234"
}
```

## Querying Flight State

The FlightWorkflow supports three query methods that allow you to inspect the current state of a running workflow without blocking its execution:

### Available Query Methods

1. **getCurrentState()** - Returns the current FlightState enum value
2. **getFlightDetails()** - Returns the complete Flight object with all current data
3. **getDelayMinutes()** - Returns the current delay in minutes (0 if not delayed)

### Example: Querying Flight State in Tests

```java
// Start a flight workflow
FlightWorkflow workflow = workflowClient.newWorkflowStub(
    FlightWorkflow.class,
    WorkflowOptions.newBuilder()
        .setTaskQueue("flight-task-queue")
        .setWorkflowId("flight-AA1234-2026-01-26")
        .build()
);

// Start workflow asynchronously
WorkflowStub.fromTyped(workflow).start(flight);

// Query current state
FlightState state = workflow.getCurrentState();
System.out.println("Current state: " + state);

// Query delay
int delay = workflow.getDelayMinutes();
System.out.println("Current delay: " + delay + " minutes");

// Query complete flight details
Flight details = workflow.getFlightDetails();
System.out.println("Flight details: " + details);

// Send a signal to update the flight
workflow.announceDelay(45);

// Query again to see the updated delay
int updatedDelay = workflow.getDelayMinutes();
System.out.println("Updated delay: " + updatedDelay + " minutes"); // Output: 45 minutes
```

### Key Features of Queries

- **Non-blocking**: Queries return immediately without affecting workflow execution
- **Read-only**: Queries cannot modify workflow state
- **Real-time**: Queries return the current state, including changes from signals
- **Consistent**: Queries are strongly consistent with the workflow's current execution state

See the test file `FlightWorkflowTest.java` for complete examples of querying workflow state.

## Failure Recovery Demonstration

One of Temporal's key value propositions is **durability** - workflows survive process restarts and continue execution from their last checkpoint. This demo includes features to showcase this capability.

### Demonstrating Temporal's Durability

The application includes a special endpoint and UI button to simulate a worker failure and demonstrate automatic recovery:

#### Via Web UI

1. Start a long-running demo flight (use flight number starting with "DEMO", e.g., "DEMO999") - these flights have 5-second delays between states for a total of ~25 seconds
2. Watch the flight progress through states: SCHEDULED → BOARDING → DEPARTED → IN_FLIGHT → LANDED → COMPLETED
3. While the flight is in progress, click the **"⚡ Simulate Failure"** button in the Active Flights panel
4. The worker will stop and restart (simulating a crash and recovery)
5. Watch the workflow automatically resume from its last checkpoint and continue to completion
6. Check the Event Log to see system messages about the restart

#### Via REST API

Trigger a worker restart programmatically:

```bash
curl -X POST http://localhost:8080/api/admin/restart-worker
```

Response:
```json
{
  "status": "SUCCESS",
  "message": "Worker restarted successfully. Workflows will resume from last checkpoint."
}
```

### What Happens During Worker Restart

1. **Worker stops** - The Temporal worker process shuts down gracefully
2. **Workflows pause** - Active workflows pause at their last checkpoint (after completing the current activity or timer)
3. **Worker restarts** - A new worker process starts and registers with Temporal
4. **Workflows resume** - All paused workflows automatically continue from their last checkpoint
5. **No data loss** - All workflow state, variables, and history are preserved

### Application Logs During Recovery

You'll see logs similar to this:

```
INFO  - Stopping worker to simulate failure...
INFO  - Worker stopped
INFO  - Restarting worker...
INFO  - Registered FlightWorkflowImpl and MultiLegFlightWorkflowImpl for task queue: flight-task-queue
INFO  - Worker restarted. Workflows will resume from last checkpoint.
INFO  - Flight DEMO999 is IN_FLIGHT (continues after restart)
```

### Key Durability Features Demonstrated

- **Workflow State Persistence**: All workflow variables and execution state are preserved
- **Signal History Preservation**: Signals sent before the restart (delays, gate changes) are retained
- **Query Availability**: You can query workflow state even during and after restart
- **Automatic Recovery**: No manual intervention needed - workflows just continue
- **No Duplicate Execution**: Workflow logic doesn't re-execute completed steps

### Testing Failure Recovery

The project includes automated tests that simulate worker restarts:

```bash
./mvnw test -Dtest=FailureRecoveryTest
```

These tests verify:
- Workflows complete successfully after worker restart
- Workflow history is preserved across restarts
- Signals sent before restart are retained

### Why This Matters

In production airline systems:
- **Worker crashes** are inevitable (OOM, hardware failures, deployments)
- **Long-running processes** (multi-hour flights) must survive restarts
- **Critical state** (gate assignments, delays, passenger counts) cannot be lost
- **Business continuity** requires workflows to resume automatically

Temporal's durability guarantee means you can deploy new code, restart workers, or recover from failures without losing track of in-flight operations.

## Workflow History and Audit Trail

One of Temporal's powerful features is its **built-in audit trail** - every workflow execution maintains a complete, immutable history of all events. This is invaluable for compliance, debugging, and understanding exactly what happened during flight operations.

### Viewing Workflow History

#### Via Web UI

1. Start a flight and let it progress through several states
2. Send some signals (announce delay, change gate)
3. Select the flight in the Active Flights list
4. Click the **"📋 View Audit Trail"** button in the Flight Details panel
5. A timeline view displays all workflow events with timestamps and descriptions
6. Click **"📥 Export as JSON"** to download the complete history

#### Via REST API

Get the complete workflow history for a flight:

```bash
curl http://localhost:8080/api/flights/AA1234/history?flightDate=2026-01-26
```

Response (example):
```json
[
  {
    "eventId": 1,
    "eventType": "EVENT_TYPE_WORKFLOW_EXECUTION_STARTED",
    "timestamp": "2026-01-26 14:32:15",
    "description": "Workflow execution started",
    "category": "lifecycle"
  },
  {
    "eventId": 5,
    "eventType": "EVENT_TYPE_TIMER_STARTED",
    "timestamp": "2026-01-26 14:32:16",
    "description": "Timer started (state transition delay)",
    "category": "timer"
  },
  {
    "eventId": 12,
    "eventType": "EVENT_TYPE_WORKFLOW_EXECUTION_SIGNALED",
    "timestamp": "2026-01-26 14:32:20",
    "description": "Signal received: announceDelay",
    "category": "signal"
  },
  {
    "eventId": 45,
    "eventType": "EVENT_TYPE_WORKFLOW_EXECUTION_COMPLETED",
    "timestamp": "2026-01-26 14:32:35",
    "description": "Workflow execution completed successfully",
    "category": "lifecycle"
  }
]
```

### Event Categories

The history events are categorized for easier understanding:

- **lifecycle** - Workflow start, completion, cancellation, termination
- **signal** - Signals received (delays, gate changes, cancellations)
- **timer** - State transition delays (using Workflow.sleep)
- **task** - Workflow task scheduling and execution
- **child_workflow** - Child workflow operations (for multi-leg journeys)
- **other** - All other internal Temporal events

### What the Audit Trail Includes

Every workflow history contains:

1. **All state transitions** - Complete record of flight lifecycle (SCHEDULED → BOARDING → DEPARTED → etc.)
2. **Signals received** - Every delay announcement, gate change, or cancellation with timestamps
3. **Activities executed** - Any activity invocations (future: crew notifications, passenger updates)
4. **Timers fired** - All sleep operations between state transitions
5. **Child workflows** - Multi-leg journey orchestration events
6. **System events** - Worker task scheduling, execution details

### Timeline View Features

The web UI timeline provides:

- **Visual indicators** - Color-coded dots for different event types
- **Chronological ordering** - Events displayed in time order
- **Event IDs** - Sequential event numbers for correlation
- **Human-readable descriptions** - Friendly explanations of technical events
- **Timestamps** - Precise timing for compliance and debugging

### Exporting History

The export functionality creates a JSON file containing:

```json
{
  "flightNumber": "AA1234",
  "exportedAt": "2026-01-26T14:35:00Z",
  "eventCount": 45,
  "history": [
    // Complete event array
  ]
}
```

This file can be:
- Archived for regulatory compliance
- Analyzed for operational insights
- Used for debugging production issues
- Shared with support teams for investigation

### Use Cases for Audit Trail

**Compliance & Regulatory**
- Prove exactly when decisions were made (gate changes, delays, cancellations)
- Demonstrate system behavior during incidents
- Maintain immutable records for audits

**Debugging & Support**
- Understand why a flight ended in an unexpected state
- See exact sequence of events leading to issues
- Identify timing problems or race conditions

**Operations & Analytics**
- Analyze typical flight progression patterns
- Measure time between state transitions
- Identify bottlenecks in flight operations

**Incident Investigation**
- Reconstruct what happened during system failures
- Verify that signals were received and processed
- Confirm workflows recovered correctly after restarts

### Why Temporal's Audit Trail is Valuable

Unlike application logs that can be lost or rotated:
- **Immutable** - History cannot be modified or deleted
- **Complete** - No gaps, every decision is recorded
- **Persistent** - Survives worker restarts and failures
- **Queryable** - Access at any time via API or UI
- **Built-in** - No extra code needed, it's automatic

For airline operations handling millions of dollars in assets and customer commitments, having a complete audit trail of every flight decision is invaluable for both operational excellence and regulatory compliance.

## Kafka Integration for Event Ingestion

The application demonstrates how Temporal complements existing streaming architectures by integrating with Apache Kafka. Flight events published to Kafka topics are automatically consumed and translated into workflow signals, enabling event-driven workflow orchestration.

### Architecture Overview

```
External Systems → Kafka (flight-events topic) → FlightEventConsumer → Temporal Workflow Signals → Flight State Updates
```

This integration shows how:
- **Kafka** handles high-throughput event streaming from external systems (radar, gate systems, crew apps)
- **Temporal** provides durable orchestration of multi-step flight processes
- **Together** they create a robust event-driven architecture where events drive workflow state machines

### Supported Event Types

The system maps Kafka events to workflow signals:

| Kafka Event Type | Workflow Signal | Description |
|-----------------|----------------|-------------|
| `DELAY_ANNOUNCED` | `announceDelay(minutes)` | Flight delay notification |
| `GATE_CHANGED` | `changeGate(newGate)` | Gate reassignment |
| `GATE_ASSIGNED` | `changeGate(gate)` | Initial gate assignment |
| `FLIGHT_CANCELLED` | `cancelFlight(reason)` | Flight cancellation |

### Event Message Format

Events published to the `flight-events` topic should follow this JSON schema:

```json
{
  "eventType": "DELAY_ANNOUNCED",
  "flightNumber": "AA1234",
  "flightDate": "2026-01-26",
  "data": "{\"delayMinutes\": 45}"
}
```

#### Example Events

**Delay Announcement:**
```json
{
  "eventType": "DELAY_ANNOUNCED",
  "flightNumber": "AA1234",
  "flightDate": "2026-01-26",
  "data": "{\"delayMinutes\": 45}"
}
```

**Gate Change:**
```json
{
  "eventType": "GATE_CHANGED",
  "flightNumber": "AA1234",
  "flightDate": "2026-01-26",
  "data": "{\"gate\": \"C12\"}"
}
```

**Flight Cancellation:**
```json
{
  "eventType": "FLIGHT_CANCELLED",
  "flightNumber": "AA1234",
  "flightDate": "2026-01-26",
  "data": "{\"reason\": \"Weather conditions\"}"
}
```

### Testing Kafka Integration

#### Manually Publishing Events

You can use the Kafka console producer to manually publish test events:

```bash
# Connect to Kafka container
docker exec -it temporal-jetstream-kafka-1 /bin/bash

# Produce a test event
kafka-console-producer --broker-list localhost:9092 --topic flight-events

# Then paste an event (Ctrl+D to exit):
{"eventType":"DELAY_ANNOUNCED","flightNumber":"AA1234","flightDate":"2026-01-26","data":"{\"delayMinutes\":30}"}
```

#### Monitoring Kafka Messages

To see messages being consumed from the flight-events topic:

```bash
# View messages in the flight-events topic
docker exec -it temporal-jetstream-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic flight-events \
  --from-beginning
```

#### Running Integration Tests

The project includes comprehensive Kafka integration tests:

```bash
# Run all Kafka integration tests
./mvnw test -Dtest=KafkaIntegrationTest

# Run specific test
./mvnw test -Dtest=KafkaIntegrationTest#testDelayEventFromKafka
```

These tests use **EmbeddedKafka** for fast, isolated testing without external dependencies.

### How It Works

1. **FlightEventConsumer** listens to the `flight-events` Kafka topic using `@KafkaListener`
2. When an event arrives, it's deserialized from JSON into a `FlightEvent` object
3. The consumer extracts the event type and relevant data (delay minutes, gate, reason)
4. Based on event type, the consumer gets the workflow stub using the flight number and date
5. The appropriate signal method is called on the workflow (`announceDelay`, `changeGate`, `cancelFlight`)
6. The workflow processes the signal and updates its state accordingly
7. State changes are broadcast to the WebSocket clients for real-time UI updates

### Application Logs

When Kafka events are processed, you'll see logs like:

```
INFO  - Received Kafka message: {"eventType":"DELAY_ANNOUNCED",...}
INFO  - Received Kafka event: DELAY_ANNOUNCED for flight AA1234
INFO  - Sent announceDelay signal to workflow flight-AA1234-2026-01-26 with 45 minutes
```

### Error Handling

The consumer handles several error scenarios gracefully:

- **Deserialization errors** - Logged with original message for debugging
- **Workflow not found** - Logged as error (flight workflow must be started before events)
- **Missing data fields** - Defaults to safe values (0 delay, empty gate, "Unknown reason")
- **Duplicate messages** - Signals are idempotent, safe to receive multiple times

### Integration with Flink

In a complete architecture, you might have:

```
Flight Events → Kafka
                  ↓
                Flink (Stream Processing)
                  ↓
            Metrics & Analytics (Kafka topic)
                  ↓
            Temporal (Orchestration)
                  ↓
            Downstream Systems
```

Where:
- **Flink** processes events for real-time metrics, aggregations, and pattern detection
- **Temporal** orchestrates long-running flight processes with durable state
- **Kafka** ties everything together as the event backbone

This demo focuses on the Temporal orchestration piece, showing how it complements (not replaces) streaming analytics.

### Why This Integration Matters

**Event-Driven Architecture**
- External systems can publish events without knowing about Temporal
- Loose coupling between event producers and workflow orchestration
- Kafka provides buffering and replay capabilities

**Idempotent Processing**
- Workflow signals are designed to be idempotent
- Safe to reprocess Kafka messages after consumer restarts
- No duplicate signal execution issues

**Temporal Complements Kafka**
- Kafka: High-throughput, low-latency event streaming
- Temporal: Durable, reliable, multi-step process orchestration
- Together: Event-driven workflows that survive failures

For airlines, this means gate systems can publish events to Kafka, and those events automatically drive flight workflows without tight coupling or complex state management.

## Verifying the Setup

Once all services are running, you should see:

- Temporal Web UI: `http://localhost:8233`
- Application: `http://localhost:8080`
- Kafka: `localhost:9092`
- MongoDB: `localhost:27017`

Check the application logs for:
```
Started Application in X seconds
```

## Technology Stack

- **Java 21** - Modern Java LTS version
- **Spring Boot 4.0.1** - Latest stable Spring Boot framework
- **Temporal Java SDK 1.32.1** - Workflow orchestration
- **Apache Kafka 7.8.0** - Event streaming
- **MongoDB 8.0** - Document persistence
- **WebSockets** - Real-time UI updates

## Project Structure

```
temporal-jetstream/
├── src/
│   ├── main/
│   │   ├── java/com/temporal/jetstream/
│   │   │   └── Application.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/temporal/jetstream/
├── pom.xml
├── docker-compose.yml
└── README.md
```

## Stopping the Services

Stop the application with `Ctrl+C`, then stop supporting services:

```bash
# Stop Docker services
docker-compose down

# Stop Temporal server
# Press Ctrl+C in the Temporal server terminal
```

## Next Steps

This is the initial project setup. Future stories will add:
- Flight workflow implementation
- Signal and query handling
- REST API endpoints
- Real-time web UI
- Kafka integration
- Failure recovery demonstrations

## Troubleshooting

### Application won't start

- Verify Java version: `java -version` (should be 21)
- Check Temporal is running: `temporal server start-dev`
- Ensure ports are available: 8080 (app), 7233 (Temporal), 9092 (Kafka), 27017 (MongoDB)

### Docker services won't start

```bash
# Remove old containers and volumes
docker-compose down -v
docker-compose up -d
```

## License

MIT
