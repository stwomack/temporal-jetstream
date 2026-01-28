# Agent Learnings for Future Iterations

This file contains consolidated learnings from implementing the Temporal Jetstream project. Use this as a reference for understanding the codebase, patterns, and gotchas.

## Codebase Patterns

### Technology Stack
- **Spring Boot 4.0.1** with Java 21
- **Temporal Java SDK 1.32.1** connecting to localhost:7233
- **Maven wrapper** (`./mvnw`) for all build commands
- Application runs on **port 8080**
- **Kafka** and **MongoDB** run via Homebrew services (NOT Temporal - it runs separately via `temporal server start-dev`)
- **Flink** runs via aliases: `start-flink` and `stop-flink` (not a brew service)

### Project Structure
```
src/main/java/com/temporal/jetstream/
├── config/          - Spring configuration classes (Temporal, WebSocket, Kafka)
├── controller/      - REST API controllers (FlightController, AdminController)
├── dto/             - Data Transfer Objects for API requests/responses
├── model/           - Domain models (Flight, FlightState, FlightEvent, etc.)
├── service/         - Services (FlightEventService, FlightEventConsumer, HistoryService, WorkerManagementService)
└── workflow/        - Temporal workflows (FlightWorkflow, MultiLegFlightWorkflow)
```

### Temporal Workflow Patterns

**Workflow Interface Pattern:**
```java
@WorkflowInterface
public interface FlightWorkflow {
    @WorkflowMethod
    Flight executeFlight(Flight flight);

    @SignalMethod
    void announceDelay(int minutes);

    @QueryMethod
    FlightState getCurrentState();
}
```

**Workflow Implementation Pattern:**
- Use `Workflow.sleep()` NOT `Thread.sleep()` for deterministic execution
- Use `Workflow.getLogger()` for logging within workflows
- Store signal data in workflow instance variables
- Use `Workflow.await()` for sophisticated signal handling with conditions

**Worker Registration Pattern:**
```java
@Configuration
public class TemporalConfig {
    @Bean
    public Worker worker(WorkerFactory workerFactory) {
        Worker worker = workerFactory.newWorker("flight-task-queue");
        worker.registerWorkflowImplementationTypes(
            FlightWorkflowImpl.class,
            MultiLegFlightWorkflowImpl.class
        );
        workerFactory.start(); // Critical: Must call start()
        return worker;
    }
}
```

**WorkflowId Pattern:**
- Format: `flight-{flightNumber}-{flightDate}` for single flights
- Format: `journey-{journeyId}` for multi-leg journeys
- Ensures uniqueness and easy lookup

### Spring Kafka Integration

**Consumer Pattern:**
```java
@Service
public class FlightEventConsumer {
    @KafkaListener(topics = "flight-events", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeFlightEvent(String message) {
        // Deserialize JSON
        // Map event type to workflow signal
        // Get workflow stub by workflowId
        // Call signal method
    }
}
```

**Event Message Format:**
```json
{
  "eventType": "DELAY_ANNOUNCED",
  "flightNumber": "AA1234",
  "flightDate": "2026-01-26",
  "data": "{\"delayMinutes\": 45}"
}
```

**Key Kafka Patterns:**
- Events have nested JSON data field that requires double parsing
- Consumer should be idempotent (handle duplicate messages safely)
- Error handling: log errors but don't crash consumer
- WorkflowNotFoundException means workflow hasn't started (not fatal)

### WebSocket Real-time Updates

**Pattern:**
```java
@Service
public class FlightEventService {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void publishFlightUpdate(Flight flight) {
        messagingTemplate.convertAndSend("/topic/flights", flight);
    }
}
```

- Uses STOMP over WebSocket with SockJS fallback
- Publish events AFTER signal operations complete
- Query workflow state before publishing to get updated data

### REST API Patterns

**Starting Workflows:**
```java
WorkflowOptions options = WorkflowOptions.newBuilder()
    .setWorkflowId(workflowId)
    .setTaskQueue("flight-task-queue")
    .build();
FlightWorkflow workflow = workflowClient.newWorkflowStub(FlightWorkflow.class, options);
WorkflowClient.start(workflow::executeFlight, flight);
```

**Sending Signals:**
```java
FlightWorkflow workflow = workflowClient.newWorkflowStub(FlightWorkflow.class, workflowId);
workflow.announceDelay(minutes);
```

**Querying State:**
```java
FlightWorkflow workflow = workflowClient.newWorkflowStub(FlightWorkflow.class, workflowId);
FlightState state = workflow.getCurrentState();
```

### Testing Patterns

**Workflow Testing with TestWorkflowEnvironment:**
```java
@ExtendWith(TestWorkflowExtension.class)
class FlightWorkflowTest {
    @Test
    void testWorkflow(TestWorkflowEnvironment testEnv, Worker worker, WorkflowClient client) {
        worker.registerWorkflowImplementationTypes(FlightWorkflowImpl.class);
        testEnv.start();
        // Test workflow...
    }
}
```

**Key Testing Learnings:**
- TestWorkflowEnvironment uses "UnitTest" namespace, not "default"
- Use `WorkflowStub.start()` to start workflows asynchronously for signal testing
- Signals can be sent immediately after starting (don't need sleep in tests)
- Query methods work on running workflows
- All 21 tests pass in ~8 seconds using in-memory Temporal server

### Common Gotchas

**Workflow Determinism:**
- NEVER use `Thread.sleep()` - use `Workflow.sleep()`
- NEVER use `System.currentTimeMillis()` - use `Workflow.currentTimeMillis()`
- NEVER use `new Random()` - use `Workflow.newRandom()`
- Conditional logic based on input parameters is fine (e.g., DEMO flights use longer sleeps)

**Worker Startup:**
- Must call `workerFactory.start()` for workers to poll for tasks
- `@PostConstruct` timing can be problematic - call `start()` directly in `@Bean` method

**Child Workflows:**
- Use `Workflow.newChildWorkflowStub()` with `ChildWorkflowOptions`
- Set `ParentClosePolicy` appropriately (import from `io.temporal.api.enums.v1`)
- Each child has independent WorkflowId
- Parent waits for child completion by calling child workflow method directly

**Signal Handling:**
- Signals are asynchronous and can arrive anytime
- Signal methods should be lightweight (no activities)
- Store signal data in instance variables
- Signals are idempotent by design

**Query Methods:**
- Must NOT modify workflow state (read-only)
- Return immediately without blocking workflow execution
- Return strongly consistent data with current execution state
- Lightweight - no heavy computation

**Workflow History:**
- Fetch using `WorkflowServiceStubs.blockingStub().getWorkflowExecutionHistory()`
- Namespace must match WorkflowClient namespace (use `workflowClient.getOptions().getNamespace()`)
- TestWorkflowEnvironment uses "UnitTest" namespace
- History can be large for long-running workflows

## Build & Test Commands

```bash
# Build
./mvnw clean install

# Run application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=FlightWorkflowTest

# Run without Kafka integration tests (embedded Kafka can be flaky)
./mvnw test -Dtest='FlightWorkflowTest,MultiLegFlightWorkflowTest,FailureRecoveryTest,HistoryServiceTest'
```

## Development Workflow

### Starting Services

```bash
# Terminal 1: Start Temporal
temporal server start-dev

# Terminal 2: Start Kafka and MongoDB via Homebrew
brew services start kafka
brew services start mongodb-community

# Terminal 3: Start Flink
start-flink

# Terminal 4: Run application
./mvnw spring-boot:run
```

### Testing Workflow Signals

```bash
# Start a flight
curl -X POST http://localhost:8080/api/flights/start \
  -H "Content-Type: application/json" \
  -d '{"flightNumber":"AA1234","flightDate":"2026-01-26",...}'

# Send signal
curl -X POST http://localhost:8080/api/flights/AA1234/delay?flightDate=2026-01-26 \
  -H "Content-Type: application/json" \
  -d '{"minutes":45}'

# Query state
curl http://localhost:8080/api/flights/AA1234/details?flightDate=2026-01-26
```

### Testing Kafka Integration

```bash
# Produce event
kafka-console-producer --broker-list localhost:9092 --topic flight-events

# Paste event:
{"eventType":"DELAY_ANNOUNCED","flightNumber":"AA1234","flightDate":"2026-01-26","data":"{\"delayMinutes\":30}"}

# Consume events (different terminal)
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic flight-events \
  --from-beginning
```

## Key Dependencies

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Temporal SDK -->
    <dependency>
        <groupId>io.temporal</groupId>
        <artifactId>temporal-sdk</artifactId>
        <version>1.32.1</version>
    </dependency>

    <!-- Spring Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>

    <!-- MongoDB -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>

    <!-- Jackson for Java 8 date/time -->
    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
    </dependency>

    <!-- Test Dependencies -->
    <dependency>
        <groupId>io.temporal</groupId>
        <artifactId>temporal-testing</artifactId>
        <version>1.32.1</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Architecture Decisions

### Why Temporal for Flight Orchestration?

**Durability:**
- Flight state survives worker crashes and deployments
- No manual checkpointing or state management required
- Automatic recovery from failures

**Long-running Workflows:**
- Flights can span multiple days (delays, diversions)
- No timeout limits on workflow duration
- Efficient sleep for hours/days without holding resources

**Event-Driven:**
- Kafka events drive workflow signals
- Loose coupling between event producers and orchestration
- Built-in replay and retry capabilities

**Audit Trail:**
- Complete, immutable history of all decisions
- Regulatory compliance out-of-the-box
- Debugging and incident investigation

### Why Kafka Integration?

**Complementary Architectures:**
- Kafka: High-throughput event streaming
- Temporal: Durable process orchestration
- Together: Event-driven workflows with guaranteed execution

**Real-world Integration:**
- External systems (gate systems, crew apps) publish to Kafka
- FlightEventConsumer translates events to workflow signals
- No tight coupling between systems

### Why WebSocket for UI Updates?

**Real-time Updates:**
- Flight state changes pushed to UI immediately
- No polling required
- Low latency for operational awareness

**STOMP Protocol:**
- Standard protocol over WebSocket
- SockJS fallback for older browsers
- Simple topic-based pub/sub model

## Future Enhancement Ideas

### Activities
- Add activities for external system interactions (crew notifications, passenger updates)
- Implement retry policies with exponential backoff
- Add circuit breakers for failing dependencies

### Advanced Workflows
- Implement ContinueAsNew for very long-running workflows
- Add workflow versioning for safe deployments
- Implement saga patterns for compensating transactions

### Observability
- Add Prometheus metrics for workflow execution times
- Integrate with Grafana for dashboards
- Add distributed tracing with OpenTelemetry

### Production Readiness
- Add authentication/authorization
- Implement rate limiting
- Add comprehensive error handling
- Deploy to Kubernetes with Helm charts

## References

- [Temporal Documentation](https://docs.temporal.io/)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Temporal Community](https://community.temporal.io/)
