# Temporal Jetstream - Airline Flight Lifecycle Orchestration Demo

A demonstration application showing how Temporal provides durability, reliability, scalability, and consistency for multi-day airline flight lifecycle orchestration. Complements existing Kafka/Flink streaming architecture by providing durable execution guarantees for long-running flight processes.

## Architecture & Value Proposition

### System Architecture

```
┌─────────────────┐
│  External       │
│  Systems        │
│  (Gate, Crew,   │
│   Weather, etc) │
└────────┬────────┘
         │ Publishes raw events
         ▼
┌─────────────────────┐
│ Kafka: raw-flight-  │
│        events       │
└──────────┬──────────┘
           │
           ▼
    ┌──────────────┐
    │    Flink     │ ← Enriches events:
    │  Enrichment  │   - estimatedDelay
    │     Job      │   - riskScore
    └──────┬───────┘   - enrichedTimestamp
           │
           ▼
┌─────────────────────┐
│ Kafka: flight-events│
└──────────┬──────────┘
           │
           ▼
    ┌──────────────┐
    │  FlightEvent │ ← Sends signals
    │   Consumer   │   to workflows
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐
    │   Temporal   │ ← Durable workflow
    │   Workflows  │   orchestration
    └──────┬───────┘
           │
    ┌──────┴──────┬──────────┐
    │             │          │
    ▼             ▼          ▼
┌────────┐  ┌─────────┐  ┌─────────┐
│MongoDB │  │  Kafka: │  │WebSocket│
│History │  │flight-  │  │ Events  │
│        │  │state-   │  │         │
│        │  │changes  │  │         │
└────────┘  └─────────┘  └────┬────┘
                              │
                              ▼
                         ┌─────────┐
                         │ Web UI  │
                         └─────────┘
```

### How Temporal Complements Kafka/Flink

**Kafka** provides:
- High-throughput, low-latency event streaming
- Event buffering and replay capabilities
- Loose coupling between producers and consumers

**Flink** provides:
- Real-time stream processing
- Complex event aggregations and analytics
- Time-windowed computations

**Temporal** provides:
- Durable, long-running process orchestration
- Guaranteed execution of multi-step workflows
- Built-in retry and failure recovery
- Complete audit trail of all decisions

Together, they create a comprehensive airline operations platform:
- **Events flow through Kafka** (gate changes, delays, crew updates)
- **Flink enriches events** (adds risk scores, delay estimates, calculated fields)
- **Temporal orchestrates workflows** (flight lifecycle, multi-leg journeys, durable execution)

### The Four 'ilities: Temporal's Value Proposition

#### 1. **Durability** - Workflows Survive Failures

**What it means:** Flight state persists across worker restarts and crashes.

**Demo feature:** Click "Simulate Failure" button while flight is in progress. Watch workflow automatically resume from last checkpoint.

**Real-world value:**
- Deploy new code without losing in-flight operations
- Recover from crashes without manual intervention
- No lost gate assignments or delay notifications

#### 2. **Reliability** - Guaranteed Execution

**What it means:** Multi-step processes complete even if individual steps fail temporarily.

**Demo feature:** Workflow progresses through all states (SCHEDULED → BOARDING → DEPARTED → IN_FLIGHT → LANDED → COMPLETED) with automatic retries on failures.

**Real-world value:**
- Crew notifications eventually succeed even if systems are temporarily down
- Passenger updates retry until delivered
- No manual tracking of "what step failed"

#### 3. **Consistency** - Complete Audit Trail

**What it means:** Every state transition, signal, and decision is recorded immutably.

**Demo feature:** Click "View Audit Trail" to see complete history with timestamps for all events, delays, gate changes, and cancellations.

**Real-world value:**
- Regulatory compliance (prove when decisions were made)
- Debugging (understand exactly what happened)
- Analytics (measure actual vs. planned timelines)

#### 4. **Scalability** - Independent Workflow Instances

**What it means:** Each flight is an independent workflow instance; horizontally scalable.

**Demo feature:** Start multiple flights simultaneously. Each has independent state and can be queried/signaled independently.

**Real-world value:**
- Handle 10,000+ concurrent flights per airport
- No shared state bottlenecks
- Scale workers up/down based on flight volume

### Key Technical Benefits

**Event-Driven Integration:**
- External systems publish to Kafka
- Temporal consumes events and drives workflows
- No tight coupling between event producers and orchestration

**State Management:**
- Workflow variables automatically persisted
- No need for external state store
- Queries provide real-time state access

**Failure Handling:**
- Automatic retries with exponential backoff
- Circuit breakers for failing dependencies
- Graceful degradation on partial failures

**Observability:**
- Complete workflow history for every flight
- Real-time queries for current state
- Integration with monitoring/alerting systems

## Prerequisites

Before running the application, ensure you have the following installed:

- **Java 21** - [Download from Oracle](https://www.oracle.com/java/technologies/downloads/#java21) or use SDKMAN
- **Temporal CLI** - For running Temporal Server locally
- **Apache Kafka** - Event streaming platform (via Homebrew)
- **MongoDB** - Document database for state persistence (via Homebrew)
- **Apache Flink** - Stream processing and event enrichment (via Homebrew)

### Installation Commands (macOS)

```bash
# Install all required services via Homebrew
brew install temporal
brew install kafka
brew install mongodb-community
brew install apache-flink
```

### Setting Up Flink Aliases (macOS)

Flink doesn't run as a brew service (no plist available), so we use aliases for convenience:

```bash
# Add these to ~/.zshrc or ~/.bash_profile
alias start-flink='/opt/homebrew/Cellar/apache-flink/VERSION/libexec/bin/start-cluster.sh'
alias stop-flink='/opt/homebrew/Cellar/apache-flink/VERSION/libexec/bin/stop-cluster.sh'

# Replace VERSION with your installed version. Check with:
ls /opt/homebrew/Cellar/apache-flink/

# Example for version 1.19.1:
alias start-flink='/opt/homebrew/Cellar/apache-flink/1.19.1/libexec/bin/start-cluster.sh'
alias stop-flink='/opt/homebrew/Cellar/apache-flink/1.19.1/libexec/bin/stop-cluster.sh'

# Reload your shell configuration
source ~/.zshrc  # or source ~/.bash_profile
```

**Important Notes:**
- Kafka via brew includes Zookeeper automatically
- MongoDB data directory: `/opt/homebrew/var/mongodb`
- Flink must be started via aliases (not a brew service)
- Adjust Flink version number in aliases based on your installation

## Quick Start

### 1. Start Temporal Server

In a separate terminal, start the Temporal development server:

```bash
temporal server start-dev
```

This will start Temporal on `localhost:7233` with the Web UI available at `http://localhost:8233`.

### 2. Start Kafka and MongoDB

Start Kafka and MongoDB using Homebrew services:

```bash
# Start Kafka (includes Zookeeper)
brew services start kafka

# Start MongoDB
brew services start mongodb-community

# Verify services are running
brew services list
```

Expected output should show kafka and mongodb-community as "started".

### 3. Start Apache Flink

Start the Flink standalone cluster using the alias:

```bash
start-flink
```

Verify Flink is running by accessing the Web UI at `http://localhost:8081`.

To stop Flink later:
```bash
stop-flink
```

### 4. Create Kafka Topics

Create the required Kafka topics for event ingestion and state change publishing:

```bash
# Create raw-flight-events topic (for external events → Flink enrichment)
kafka-topics --create \
  --topic raw-flight-events \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1

# Create flight-events topic (for Flink enriched events → workflows)
kafka-topics --create \
  --topic flight-events \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1

# Create flight-state-changes topic (for workflow state changes → downstream systems)
kafka-topics --create \
  --topic flight-state-changes \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1

# Verify topics were created
kafka-topics --list --bootstrap-server localhost:9092
```

**Note:** Kafka auto-creates topics by default, but explicit creation ensures proper configuration.

### 5. Build the Application

```bash
./mvnw clean install
```

### 6. Build and Submit Flink Job (Separate Project)

The Flink enrichment job is in a separate project: `../flink-enrichment`

```bash
# Build the Flink project
cd ../flink-enrichment
mvn clean package

# Submit to Flink cluster
flink run -c com.temporal.flink.FlinkEnrichmentJob target/flink-enrichment-job.jar

# Return to temporal-jetstream
cd ../temporal-jetstream
```

Verify the job is running in the Flink Web UI at `http://localhost:8081`.

To list running jobs:
```bash
flink list
```

To cancel a job:
```bash
flink cancel <job-id>
```

### 7. Run the Application

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8082`.

## Workflow Timing

All flights use fixed 20-second phase durations for fast, predictable demos:

| Phase | Duration |
|-------|----------|
| SCHEDULED → BOARDING | 20 seconds |
| BOARDING → DEPARTED | 20 seconds |
| DEPARTED → IN_FLIGHT | 20 seconds |
| IN_FLIGHT → LANDED | 20 seconds |
| LANDED → COMPLETED | 20 seconds |

**Total Flight Duration:** ~100 seconds (under 2 minutes)

This timing is optimized for:
- **Live presentations** - Complete flight lifecycle quickly
- **Development/testing** - Fast feedback cycles
- **Chaos testing** - Enough time to kill services and watch recovery

## Complete Demo Script (~5 minutes)

This step-by-step script demonstrates all key features of the system. Follow along to see Temporal's durability, reliability, consistency, and scalability in action.

### Step 1: Verify All Services Are Running

Before starting the demo, ensure all services are healthy:

```bash
# Check Temporal is accessible
temporal namespace list

# Check Homebrew services
brew services list
# Should see: kafka and mongodb-community with status "started"

# Check Flink is running
curl http://localhost:8081

# Check application is running
curl http://localhost:8082/api/flights/AA1234/state?flightDate=2026-01-26 || echo "App ready for flights"
```

Open these URLs in separate browser tabs:
- **Application UI:** http://localhost:8082
- **Temporal Web UI:** http://localhost:8233
- **Flink Web UI:** http://localhost:8081

### Step 2: Start a Flight Workflow

**Via Web UI:**
1. Open http://localhost:8082
2. Fill in the "Start New Flight" form:
   - Flight Number: `AA1234`
   - Departure Station: `ORD`
   - Arrival Station: `DFW`
   - Gate: `A12`
   - Aircraft: `N123AA`
3. Click "Start Flight"
4. Watch the flight appear in the Active Flights list

**Via REST API:**
```bash
curl -X POST http://localhost:8082/api/flights/start \
  -H "Content-Type: application/json" \
  -d '{
    "flightNumber": "AA1234",
    "flightDate": "2026-01-26",
    "departureStation": "ORD",
    "arrivalStation": "DFW",
    "scheduledDeparture": "2026-01-26T14:00:00",
    "scheduledArrival": "2026-01-26T16:30:00",
    "gate": "A12",
    "aircraft": "N123AA"
  }'
```

**Expected Result:** Flight workflow starts and progresses through states every 20 seconds: SCHEDULED → BOARDING → DEPARTED → IN_FLIGHT → LANDED → COMPLETED (~100 seconds total)

### Step 3: Send Events to Update Flight State

While the flight is in progress, send signals to update its state.

**Announce a Delay:**

Via UI: Click "Announce Delay" and enter 45 minutes

Via API:
```bash
curl -X POST http://localhost:8082/api/flights/AA1234/delay?flightDate=2026-01-26 \
  -H "Content-Type: application/json" \
  -d '{"minutes": 45}'
```

**Change the Gate:**

Via UI: Click "Change Gate" and enter `B24`

Via API:
```bash
curl -X POST http://localhost:8082/api/flights/AA1234/gate?flightDate=2026-01-26 \
  -H "Content-Type: application/json" \
  -d '{"newGate": "B24"}'
```

**Expected Result:** Watch the UI update in real-time with the new delay and gate information. Check the Event Log for signal events.

### Step 4: Query Flight State

Verify the workflow's current state using queries.

**Via REST API:**
```bash
# Get current state
curl http://localhost:8082/api/flights/AA1234/state?flightDate=2026-01-26

# Get complete flight details
curl http://localhost:8082/api/flights/AA1234/details?flightDate=2026-01-26
```

**Expected Result:** JSON response showing current state (e.g., IN_FLIGHT), gate (B24), and delay (45 minutes).

### Step 5: Demonstrate Failure Recovery (Durability)

This step shows Temporal's durability - workflows survive process restarts.

**Start a Flight:**

Via UI: Start any flight (e.g., `TEST999`). Flights take ~100 seconds to complete (20 seconds per phase).

**Simulate a Worker Failure:**

Via UI: While the flight is in progress, click the "⚡ Simulate Failure" button in the Active Flights panel

Via API:
```bash
curl -X POST http://localhost:8082/api/admin/restart-worker
```

**Watch What Happens:**
1. Application logs show: "Worker stopped"
2. Application logs show: "Worker restarted"
3. Workflow automatically resumes from last checkpoint
4. Flight continues: IN_FLIGHT → LANDED → COMPLETED
5. No data loss - delay and gate changes are preserved

**Expected Result:** The flight completes successfully despite the worker restart, demonstrating that workflow state is durable and survives failures.

### Step 6: View Complete Audit Trail (Consistency)

Every workflow maintains a complete, immutable history of all events.

**Via Web UI:**
1. Select a completed flight (e.g., AA1234)
2. Click "📋 View Audit Trail" button
3. See timeline view with all events:
   - Workflow started
   - State transitions (SCHEDULED → BOARDING → etc.)
   - Signals received (delay announced, gate changed)
   - Workflow completed
4. Click "📥 Export as JSON" to download the history

**Via REST API:**
```bash
curl http://localhost:8082/api/flights/AA1234/history?flightDate=2026-01-26
```

**Expected Result:** Complete chronological history with timestamps showing every decision, state change, and signal. This immutable audit trail is valuable for compliance and debugging.

### Step 7: Test Kafka Integration (Event-Driven Architecture)

Demonstrate how external events from Kafka automatically drive workflow signals.

**Publish an Event to Kafka:**
```bash
# Start the Kafka console producer
kafka-console-producer --broker-list localhost:9092 --topic flight-events

# Paste this event (then press Enter and Ctrl+D):
{"eventType":"DELAY_ANNOUNCED","flightNumber":"AA1234","flightDate":"2026-01-26","data":"{\"delayMinutes\":30}"}
```

**Watch the Application:**
1. Check application logs: Should see "Received Kafka event: DELAY_ANNOUNCED for flight AA1234"
2. Check application logs: Should see "Sent announceDelay signal to workflow"
3. Check UI: Flight AA1234 should now show 30-minute delay

**Expected Result:** Kafka events are automatically consumed and translated into workflow signals, showing how Temporal integrates with streaming architectures.

### Step 8: Test Multi-Leg Journey (Scalability)

Demonstrate child workflow orchestration for connecting flights.

**Start a Multi-Leg Journey:**
```bash
curl -X POST http://localhost:8082/api/flights/journey \
  -H "Content-Type: application/json" \
  -d '{
    "flights": [
      {
        "flightNumber": "AA100",
        "flightDate": "2026-01-26",
        "departureStation": "ORD",
        "arrivalStation": "DFW",
        "scheduledDeparture": "2026-01-26T08:00:00",
        "scheduledArrival": "2026-01-26T11:00:00",
        "gate": "A1",
        "aircraft": "N100AA"
      },
      {
        "flightNumber": "AA200",
        "flightDate": "2026-01-26",
        "departureStation": "DFW",
        "arrivalStation": "LAX",
        "scheduledDeparture": "2026-01-26T12:00:00",
        "scheduledArrival": "2026-01-26T14:00:00",
        "gate": "B1",
        "aircraft": "N100AA"
      },
      {
        "flightNumber": "AA300",
        "flightDate": "2026-01-26",
        "departureStation": "LAX",
        "arrivalStation": "SFO",
        "scheduledDeparture": "2026-01-26T15:00:00",
        "scheduledArrival": "2026-01-26T16:30:00",
        "gate": "C1",
        "aircraft": "N100AA"
      }
    ]
  }'
```

**Expected Result:** Parent workflow orchestrates three child workflows (AA100, AA200, AA300) sequentially. Each leg waits for the previous to complete. Aircraft N100AA is transferred between legs. Watch all three flights appear in the UI.

### Demo Complete! 🎉

You've now seen all four value propositions in action:

✅ **Durability** - Workflow survived worker restart without data loss
✅ **Reliability** - Multi-step flight lifecycle completed with automatic retries
✅ **Consistency** - Complete audit trail of all events and decisions
✅ **Scalability** - Multiple independent flights and multi-leg journeys

### Additional Exploration

**Temporal Web UI:**
- Visit http://localhost:8233
- Click on "Workflows" to see all running/completed workflows
- Click a workflow ID to see detailed execution history
- Explore the workflow graph visualization

**Cancel a Flight:**
```bash
curl -X POST http://localhost:8082/api/flights/AA1234/cancel?flightDate=2026-01-26 \
  -H "Content-Type: application/json" \
  -d '{"reason": "Weather conditions"}'
```

**Monitor Kafka:**
```bash
# View all messages in flight-events topic
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic flight-events \
  --from-beginning
```



## Web UI

The application includes an embedded web UI for real-time flight monitoring and control.

### Accessing the UI

Open your browser and navigate to:

```
http://localhost:8082
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
curl -X POST http://localhost:8082/api/flights/start \
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
curl -X POST http://localhost:8082/api/flights/AA1234/delay?flightDate=2026-01-26 \
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
curl -X POST http://localhost:8082/api/flights/AA1234/gate?flightDate=2026-01-26 \
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
curl -X POST http://localhost:8082/api/flights/AA1234/cancel?flightDate=2026-01-26 \
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
curl http://localhost:8082/api/flights/AA1234/state?flightDate=2026-01-26
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
curl http://localhost:8082/api/flights/AA1234/details?flightDate=2026-01-26
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

1. Start a flight (e.g., "TEST999") - flights progress through states every 20 seconds (~100 seconds total)
2. Watch the flight progress through states: SCHEDULED → BOARDING → DEPARTED → IN_FLIGHT → LANDED → COMPLETED
3. While the flight is in progress, click the **"⚡ Simulate Failure"** button in the Active Flights panel
4. The worker will stop and restart (simulating a crash and recovery)
5. Watch the workflow automatically resume from its last checkpoint and continue to completion
6. Check the Event Log to see system messages about the restart

#### Via REST API

Trigger a worker restart programmatically:

```bash
curl -X POST http://localhost:8082/api/admin/restart-worker
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
INFO  - Flight TEST999 is IN_FLIGHT (continues after restart)
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
curl http://localhost:8082/api/flights/AA1234/history?flightDate=2026-01-26
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

## Kafka Integration - Producer and Consumer

The application demonstrates how Temporal complements existing streaming architectures by integrating with Apache Kafka in both directions: consuming external events to drive workflows AND publishing state changes for downstream systems.

### Architecture Overview

```
External Systems → Kafka (flight-events) → FlightEventConsumer → Workflow Signals → State Changes
                                                                                         ↓
                                                                    FlightEventActivity (via Activity)
                                                                                         ↓
                                                                    Kafka (flight-state-changes) → Downstream Systems
```

This bidirectional integration shows how:
- **Kafka Consumer**: External events (delays, gate changes) drive workflow state transitions via signals
- **Kafka Producer**: Every workflow state transition publishes events for downstream analytics and monitoring
- **Temporal**: Provides durable orchestration with complete audit trail
- **Together**: They create a robust event-driven architecture with visibility into all state changes

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
# Start the console producer
kafka-console-producer --broker-list localhost:9092 --topic flight-events

# Then paste an event (Ctrl+D to exit):
{"eventType":"DELAY_ANNOUNCED","flightNumber":"AA1234","flightDate":"2026-01-26","data":"{\"delayMinutes\":30}"}
```

#### Monitoring Kafka Messages

To see messages being consumed from the flight-events topic:

```bash
# View incoming events (external systems → workflows)
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic flight-events \
  --from-beginning
```

To see state changes published by workflows:

```bash
# View outgoing state changes (workflows → downstream systems)
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic flight-state-changes \
  --from-beginning
```

**State change event format:**
```json
{
  "flightNumber": "AA1234",
  "previousState": "SCHEDULED",
  "newState": "BOARDING",
  "timestamp": "2026-01-27T10:30:00",
  "gate": "B12",
  "delay": 0
}
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

### How the Consumer Works (Event Ingestion)

1. **FlightEventConsumer** listens to the `flight-events` Kafka topic using `@KafkaListener`
2. When an event arrives, it's deserialized from JSON into a `FlightEvent` object
3. The consumer extracts the event type and relevant data (delay minutes, gate, reason)
4. Based on event type, the consumer gets the workflow stub using the flight number and date
5. The appropriate signal method is called on the workflow (`announceDelay`, `changeGate`, `cancelFlight`)
6. The workflow processes the signal and updates its state accordingly
7. State changes are broadcast to the WebSocket clients for real-time UI updates

### How the Producer Works (State Change Publishing)

1. **FlightWorkflowImpl** uses `FlightEventActivity` (Temporal Activity) to publish state changes
2. After each state transition (SCHEDULED → BOARDING → DEPARTED, etc.), the workflow calls the activity
3. **FlightEventActivityImpl** invokes **FlightEventProducer** service
4. **FlightEventProducer** serializes state change to JSON and publishes to `flight-state-changes` Kafka topic
5. Published events include: flightNumber, previousState, newState, timestamp, gate, delay
6. Activities ensure Kafka publishing happens outside the workflow (non-deterministic operations)
7. REST API signals (via FlightController) also publish to Kafka when they update flight state

**Why Activities?**
- Workflows must be deterministic (no external calls like Kafka directly from workflow code)
- Activities handle side effects like publishing to external systems
- Activities have retry policies configured for resilience
- If Kafka is unavailable, activity retries ensure eventual delivery

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

## Flink Stream Processing Integration

The application demonstrates the architectural pattern where **Flink handles stateful stream processing and enrichment** while **Temporal handles durable orchestration**. This separation of concerns creates a robust, scalable system.

### Architecture Overview

```
External Systems (Gate, Crew, Weather)
        ↓
   [raw-flight-events topic]
        ↓
   Flink Enrichment Job ← Stateless map() function
        ↓                  - Adds estimatedDelay
        ↓                  - Calculates riskScore
        ↓                  - Adds enrichedTimestamp
        ↓
   [flight-events topic]
        ↓
   FlightEventConsumer → Sends signals to workflows
        ↓
   Temporal Workflows (Durable orchestration)
```

### How It Works

1. **External systems publish raw events** to the `raw-flight-events` Kafka topic (e.g., delay announcements, gate changes)
2. **Flink consumes raw events** and enriches them with calculated fields:
   - `estimatedDelay`: Parsed from event data or calculated based on event type
   - `riskScore`: Risk level (LOW, MEDIUM, HIGH) based on delay thresholds
   - `enrichedTimestamp`: When the enrichment occurred
3. **Flink publishes enriched events** to the `flight-events` topic
4. **FlightEventConsumer** (Spring Kafka) reads enriched events and sends signals to Temporal workflows
5. **Temporal workflows** process signals and update flight state

### Enrichment Logic

The Flink job implements simple enrichment for demo purposes:

```java
// Risk Score Calculation
if (estimatedDelay > 60 minutes) -> HIGH risk
else if (estimatedDelay > 30 minutes) -> MEDIUM risk
else -> LOW risk
```

For production systems, Flink enrichment might include:
- Weather data correlation
- Historical delay pattern analysis
- Crew availability checks
- Airport capacity calculations
- Passenger connection impact

### Building and Running the Flink Job

The Flink enrichment job is in a separate project: `../flink-enrichment`

#### Build the Flink JAR

```bash
cd ../flink-enrichment
mvn clean package
```

This creates `target/flink-enrichment-job.jar` with all dependencies bundled (via Maven Shade Plugin).

#### Start Flink Cluster

```bash
start-flink
```

Verify Flink Web UI is accessible at `http://localhost:8081`.

#### Submit the Job

```bash
flink run -c com.temporal.flink.FlinkEnrichmentJob target/flink-enrichment-job.jar
```

#### Verify Job is Running

Check the Flink Web UI at `http://localhost:8081` - you should see "Flight Event Enrichment Job" in the running jobs list.

Or use CLI:
```bash
flink list
```

### Testing the Integration

#### 1. Publish a Raw Event to Kafka

```bash
# Publish a delay event to raw-flight-events topic
echo '{
  "eventType": "DELAY_ANNOUNCED",
  "flightNumber": "AA1234",
  "flightDate": "2026-01-27",
  "data": "{\"delayMinutes\":45}"
}' | kafka-console-producer --broker-list localhost:9092 --topic raw-flight-events
```

#### 2. Verify Enriched Event in flight-events Topic

```bash
# Monitor flight-events topic to see enriched event
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic flight-events \
  --from-beginning
```

You should see the enriched event with additional fields:
```json
{
  "eventType": "DELAY_ANNOUNCED",
  "flightNumber": "AA1234",
  "flightDate": "2026-01-27",
  "data": "{\"delayMinutes\":45}",
  "estimatedDelay": 45,
  "riskScore": "MEDIUM",
  "enrichedTimestamp": "2026-01-27T10:30:00"
}
```

#### 3. Verify Workflow Received Signal

Check application logs for:
```
Received Kafka event: DELAY_ANNOUNCED for flight AA1234
Sent signal to workflow: flight-AA1234-2026-01-27
```

Or query the workflow state:
```bash
curl http://localhost:8082/api/flights/AA1234/details?flightDate=2026-01-27
```

Should show `delay: 45` in the response.

### Managing the Flink Job

#### List Running Jobs
```bash
flink list
```

#### Cancel a Job
```bash
flink cancel <job-id>
```

#### Stop Flink Cluster
```bash
stop-flink
```

### Why Flink + Temporal?

**Flink** excels at:
- High-throughput stream processing (millions of events/second)
- Stateful computations (aggregations, windowing)
- Real-time analytics and metrics
- Event-time processing with watermarks

**Temporal** excels at:
- Durable, long-running workflows (hours to days)
- Multi-step process orchestration
- Guaranteed execution with retries
- Complete audit trail of decisions

**Together** they provide:
- **Stream Processing**: Flink enriches and aggregates events in real-time
- **Orchestration**: Temporal coordinates multi-step flight operations
- **Durability**: Workflows survive failures and resume automatically
- **Observability**: Complete history of both events and workflow decisions

### Architecture Notes

- **Stateless Enrichment**: The Flink job uses a simple `map()` function (no state management needed for this demo)
- **Loose Coupling**: Flink and Temporal communicate asynchronously via Kafka topics
- **Independent Scaling**: Flink and Temporal workers can scale independently based on load
- **Failure Isolation**: Flink restart doesn't affect running Temporal workflows

For airlines:
- **Flink processes real-time metrics**: On-time performance, capacity utilization, delay patterns
- **Temporal orchestrates operations**: Flight lifecycle, crew scheduling, passenger rebooking
- **Kafka connects them**: Event-driven, loosely coupled, highly scalable architecture

### Flink Web UI

Access the Flink dashboard at `http://localhost:8081` to monitor:
- Running jobs and their status
- Task parallelism and distribution
- Backpressure and throughput metrics
- Checkpoint and savepoint information
- Job execution timeline

## MongoDB Persistence for Flight State History

The application uses MongoDB to persist every flight state transition for historical analysis and debugging. This provides business state history that complements Temporal's workflow execution history.

### Architecture

```
Workflow State Transition
    ↓
PersistenceActivity (Temporal Activity)
    ↓
FlightStateTransitionRepository
    ↓
MongoDB (flight_state_transitions collection)
```

### FlightStateTransition Schema

Each state transition document in MongoDB contains:

```json
{
  "_id": "65b8f3e7a2c4d1e8f9b0c1d2",
  "flightNumber": "AA1234",
  "flightDate": "2026-01-27",
  "fromState": "SCHEDULED",
  "toState": "BOARDING",
  "timestamp": "2026-01-27T10:30:45",
  "gate": "B12",
  "delay": 15,
  "aircraft": "N12345",
  "eventType": "STATE_TRANSITION",
  "eventDetails": "Flight transitioned from SCHEDULED to BOARDING"
}
```

### Key Features

**Indexed Fields for Performance**
- `flightNumber` - Quickly find transitions for specific flight
- `flightDate` - Filter by date
- `timestamp` - Sort transitions chronologically

**Complete State History**
- Every workflow state transition is captured
- Includes contextual data: gate, delay, aircraft
- Sorted by timestamp descending (most recent first)

**Separate from Temporal History**
- Temporal provides workflow execution history (events, tasks, activities)
- MongoDB provides business state history optimized for analytics
- Both audit trails complement each other for different use cases

### REST API Endpoint

```bash
# Get all state transitions for a specific flight
GET /api/flights/{flightNumber}/transition-history

# Get transitions for specific flight on specific date
GET /api/flights/{flightNumber}/transition-history?flightDate=2026-01-27
```

**Example Response:**
```json
[
  {
    "id": "65b8f3e7a2c4d1e8f9b0c1d2",
    "flightNumber": "AA1234",
    "flightDate": "2026-01-27",
    "fromState": "LANDED",
    "toState": "COMPLETED",
    "timestamp": "2026-01-27T14:30:00",
    "gate": "B12",
    "delay": 15,
    "aircraft": "N12345",
    "eventType": "STATE_TRANSITION",
    "eventDetails": "Flight transitioned from LANDED to COMPLETED"
  },
  {
    "id": "65b8f3e7a2c4d1e8f9b0c1d1",
    "flightNumber": "AA1234",
    "flightDate": "2026-01-27",
    "fromState": "IN_FLIGHT",
    "toState": "LANDED",
    "timestamp": "2026-01-27T14:00:00",
    "gate": "B12",
    "delay": 15,
    "aircraft": "N12345",
    "eventType": "STATE_TRANSITION",
    "eventDetails": "Flight transitioned from IN_FLIGHT to LANDED"
  }
]
```

### Web UI Integration

The UI displays MongoDB transition history alongside Temporal workflow history:

1. Click on any flight in the Active Flights or Recent Flights panel
2. Click "View Audit Trail" button in the Flight Details section
3. Use the tabs to switch between:
   - **Temporal Workflow History** - Complete workflow execution events
   - **MongoDB State Transitions** - Business state changes with context

This side-by-side comparison demonstrates:
- Temporal's detailed execution history (every workflow event)
- MongoDB's focused business state history (state transitions only)

### MongoDB Setup

**Prerequisites:**
```bash
# Start MongoDB via Homebrew
brew services start mongodb-community

# Verify MongoDB is running
brew services list | grep mongodb
```

**Database Configuration:**
- Database name: `temporal-jetstream`
- Collection: `flight_state_transitions`
- Connection string: `mongodb://localhost:27017/temporal-jetstream`
- Data directory: `/opt/homebrew/var/mongodb`

**Configuration in application.yml:**
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/temporal-jetstream
      database: temporal-jetstream
```

### Testing MongoDB Persistence

```bash
# Run MongoDB integration tests
./mvnw test -Dtest=MongoDBPersistenceTest

# Tests verify:
# - State transitions are correctly saved during workflow execution
# - Query methods return transitions in correct order
# - Multiple flights maintain separate transition histories
```

### Use Cases

**Historical Analysis**
- Analyze flight patterns across dates
- Calculate average delays by route or time of day
- Identify bottlenecks in flight operations

**Debugging**
- Reconstruct exact sequence of state changes
- Correlate state transitions with external events
- Compare actual vs. scheduled timing

**Compliance & Auditing**
- Immutable record of all state changes
- Timestamp-based audit trail
- Queryable by flight, date, or state

**Business Intelligence**
- Export data to analytics platforms
- Build dashboards showing flight metrics
- Track operational KPIs

### Why Both Temporal History AND MongoDB?

**Temporal Workflow History:**
- Complete execution audit (every event, activity, signal)
- Optimized for workflow replay and debugging
- Tied to workflow lifecycle
- Best for understanding "how the workflow executed"

**MongoDB State Transitions:**
- Focused on business state changes only
- Optimized for time-series queries and analytics
- Persistent beyond workflow completion
- Best for understanding "what happened to the flight"

**Example:**
- Temporal history shows: "Signal received at 10:30:45, Activity started, Activity completed"
- MongoDB history shows: "Flight AA1234 transitioned from SCHEDULED to BOARDING at 10:30:45, gate B12, 15 min delay"

Both provide value - Temporal for workflow debugging, MongoDB for business analytics.

## Verifying the Setup

Once all services are running, you should see:

- Temporal Web UI: `http://localhost:8233`
- Application: `http://localhost:8082`
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
- **Apache Kafka** (via Homebrew) - Event streaming
- **MongoDB** (via Homebrew) - Document persistence
- **Apache Flink** (via Homebrew) - Stream processing
- **WebSockets** - Real-time UI updates

## Stopping the Services

Stop the application with `Ctrl+C`, then stop supporting services:

```bash
# Stop Homebrew services
brew services stop kafka
brew services stop mongodb-community

# Stop Flink cluster
stop-flink

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

## Demo & Test Scripts

The project includes several scripts for testing and demonstrating Temporal's capabilities.

### Chaos Testing - Launch Multiple Flights

Launch many flights simultaneously to test Temporal's durability under chaos conditions (killing services, restarting workers, etc.):

```bash
./chaos-flights.sh                    # Launch flights against localhost:8082
./chaos-flights.sh http://host:port   # Custom URL
```

**What it does:**
- Launches flights in parallel (CHS001, CHS002, etc.)
- Uses variety of airports (ORD, DFW, LAX, JFK, ATL, etc.)
- Prints chaos testing commands at the end

**Chaos testing suggestions:**
```bash
# Kill MongoDB while flights are running
brew services stop mongodb-community

# Kill Kafka
brew services stop kafka

# Use "Simulate Failure" button in UI to restart worker

# Kill the Spring Boot app (Ctrl+C) and restart it
```

Watch the Temporal UI at http://localhost:8233 - workflows will pause and automatically resume when services come back!

### Multi-Leg Journey Demo

Demonstrate parent-child workflow orchestration with a 3-leg connecting flight:

```bash
./demo-multileg.sh                    # Launch against localhost:8082
./demo-multileg.sh http://host:port   # Custom URL
```

**What it creates:**
- A 3-leg journey: ORD → DFW → PHX → LAX
- Parent workflow orchestrates 3 child workflows
- Sequential execution (Leg 2 waits for Leg 1 to complete)
- Aircraft handoff between legs

**Key demonstrations:**
- Parent-child workflow pattern
- Cascade cancellation (cancel one leg → subsequent legs auto-cancel)
- Temporal UI shows workflow hierarchy

**Timeline:** ~5 minutes total (100s per leg + turnaround)

### Start Flink Enrichment Job

Start the optional Flink stream processing job for event enrichment (from the separate `flink-enrichment` project):

```bash
cd ../flink-enrichment
./start-flink-job.sh
```

Or submit to a running Flink cluster:

```bash
cd ../flink-enrichment
flink run -c com.temporal.flink.FlinkEnrichmentJob target/flink-enrichment-job.jar
```

**What it does:**
- Consumes from `raw-flight-events` Kafka topic
- Enriches events with calculated fields (estimatedDelay, riskScore)
- Produces to `flight-events` topic

**Note:** The Flink job is optional - the app works without it. Events can go directly to `flight-events`.

## Troubleshooting

### Application won't start

- Verify Java version: `java -version` (should be 21)
- Check Temporal is running: `temporal server start-dev`
- Ensure ports are available: 8082 (app), 7233 (Temporal), 9092 (Kafka), 27017 (MongoDB), 8081 (Flink)
- Verify all services are running: `brew services list`

### Services won't start

```bash
# Restart Kafka and MongoDB
brew services restart kafka
brew services restart mongodb-community

# Check service status
brew services list

# Check service logs
tail -f /opt/homebrew/var/log/kafka/server.log
tail -f /opt/homebrew/var/log/mongodb/mongo.log
```

### Flink cluster won't start

```bash
# Stop existing cluster
stop-flink

# Check for conflicting processes on port 8081
lsof -i :8081

# Start fresh
start-flink

# Verify Flink Web UI is accessible
curl http://localhost:8081
```

### Tests failing

```bash
# Run without Kafka integration tests (they can be flaky with embedded Kafka)
./mvnw test -Dtest='FlightWorkflowTest,MultiLegFlightWorkflowTest,FailureRecoveryTest,HistoryServiceTest'
```

### Kafka consumer not receiving messages

- Verify Kafka is running: `brew services list | grep kafka`
- Check application logs for "Received Kafka message"
- Ensure flight workflow is started before publishing events (workflow must exist to receive signals)
- Verify topics exist: `kafka-topics --list --bootstrap-server localhost:9092`

### MongoDB connection errors

- Verify MongoDB is running: `brew services list | grep mongodb`
- Check MongoDB logs: `tail -f /opt/homebrew/var/log/mongodb/mongo.log`
- Verify connection: `mongosh mongodb://localhost:27017/temporal-jetstream`
- Check data directory permissions: `ls -la /opt/homebrew/var/mongodb`

### Default Ports

- **Zookeeper (Kafka)**: 8080 (admin server)
- **Flink Web UI**: 8081
- **Application**: 8082
- **Temporal Server**: 7233
- **Temporal Web UI**: 8233
- **Kafka**: 9092
- **MongoDB**: 27017

**Note:** Zookeeper's admin server uses port 8080 by default, and Flink uses 8081, so the Spring Boot application is configured to use port 8082 to avoid conflicts.

## Contributing

Contributions are welcome! This is a demonstration project showing Temporal best practices for airline operations.

### Development Setup

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make your changes
4. Run tests: `./mvnw test`
5. Commit your changes: `git commit -am 'Add new feature'`
6. Push to the branch: `git push origin feature/my-feature`
7. Submit a pull request

### Areas for Contribution

**New Features:**
- Activities for crew notifications, passenger updates
- Integration with additional external systems
- Additional workflow patterns (delays, diversions, maintenance)
- Enhanced UI with more visualizations

**Improvements:**
- Performance optimization for high-throughput scenarios
- Additional test coverage
- Documentation enhancements
- Example deployments (Kubernetes, cloud platforms)

**Bug Fixes:**
- Report issues via GitHub Issues
- Include reproduction steps and logs
- Submit PR with fix and test

### Code Style

- Follow Java standard conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Include unit tests for new features
- Update README for user-facing changes

### Questions or Ideas?

- Open a GitHub Issue for discussion
- Check existing issues for similar topics
- Temporal community: https://community.temporal.io

## License

MIT
