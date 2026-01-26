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
