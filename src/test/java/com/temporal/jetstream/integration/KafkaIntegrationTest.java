package com.temporal.jetstream.integration;

import com.temporal.jetstream.kafka.KafkaTestProducer;
import com.temporal.jetstream.model.Flight;
import com.temporal.jetstream.model.FlightState;
import com.temporal.jetstream.workflow.FlightWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Kafka event ingestion and workflow signal integration.
 * Uses embedded Kafka for testing without external dependencies.
 */
@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = {"flight-events"})
class KafkaIntegrationTest {

    @Autowired
    private WorkflowClient workflowClient;

    @Autowired
    private KafkaTestProducer kafkaTestProducer;

    @Test
    void testDelayEventFromKafka() throws Exception {
        // Given: Start a flight workflow
        String flightNumber = "KF100";
        LocalDate flightDate = LocalDate.now();
        Flight flight = createTestFlight(flightNumber, flightDate);

        String workflowId = "flight-" + flightNumber + "-" + flightDate;
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue("flight-task-queue")
                .build();

        FlightWorkflow workflow = workflowClient.newWorkflowStub(FlightWorkflow.class, options);
        WorkflowClient.start(workflow::executeFlight, flight);

        // Wait for workflow to start
        Thread.sleep(500);

        // When: Publish delay event to Kafka
        kafkaTestProducer.publishDelayEvent(flightNumber, flightDate, 45);

        // Give time for Kafka consumer to process message and send signal
        Thread.sleep(2000);

        // Then: Verify workflow received the delay signal
        int delayMinutes = workflow.getDelayMinutes();
        assertEquals(45, delayMinutes, "Workflow should have received delay signal from Kafka event");
    }

    @Test
    void testGateChangeEventFromKafka() throws Exception {
        // Given: Start a flight workflow
        String flightNumber = "KF200";
        LocalDate flightDate = LocalDate.now();
        Flight flight = createTestFlight(flightNumber, flightDate);

        String workflowId = "flight-" + flightNumber + "-" + flightDate;
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue("flight-task-queue")
                .build();

        FlightWorkflow workflow = workflowClient.newWorkflowStub(FlightWorkflow.class, options);
        WorkflowClient.start(workflow::executeFlight, flight);

        // Wait for workflow to start
        Thread.sleep(500);

        // When: Publish gate change event to Kafka
        kafkaTestProducer.publishGateChangeEvent(flightNumber, flightDate, "C12");

        // Give time for Kafka consumer to process message and send signal
        Thread.sleep(2000);

        // Then: Verify workflow received the gate change signal
        Flight details = workflow.getFlightDetails();
        assertEquals("C12", details.getGate(), "Workflow should have received gate change signal from Kafka event");
    }

    @Test
    void testCancellationEventFromKafka() throws Exception {
        // Given: Start a flight workflow
        String flightNumber = "KF300";
        LocalDate flightDate = LocalDate.now();
        Flight flight = createTestFlight(flightNumber, flightDate);

        String workflowId = "flight-" + flightNumber + "-" + flightDate;
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue("flight-task-queue")
                .build();

        FlightWorkflow workflow = workflowClient.newWorkflowStub(FlightWorkflow.class, options);
        WorkflowClient.start(workflow::executeFlight, flight);

        // Wait for workflow to start
        Thread.sleep(500);

        // When: Publish cancellation event to Kafka
        kafkaTestProducer.publishCancellationEvent(flightNumber, flightDate, "Weather conditions");

        // Give time for Kafka consumer to process message and send signal
        Thread.sleep(2000);

        // Then: Verify workflow received the cancellation signal
        FlightState currentState = workflow.getCurrentState();
        assertEquals(FlightState.CANCELLED, currentState, "Workflow should have been cancelled via Kafka event");
    }

    @Test
    void testMultipleEventsFromKafka() throws Exception {
        // Given: Start a flight workflow
        String flightNumber = "KF400";
        LocalDate flightDate = LocalDate.now();
        Flight flight = createTestFlight(flightNumber, flightDate);

        String workflowId = "flight-" + flightNumber + "-" + flightDate;
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue("flight-task-queue")
                .build();

        FlightWorkflow workflow = workflowClient.newWorkflowStub(FlightWorkflow.class, options);
        WorkflowClient.start(workflow::executeFlight, flight);

        // Wait for workflow to start
        Thread.sleep(500);

        // When: Publish multiple events to Kafka
        kafkaTestProducer.publishGateAssignedEvent(flightNumber, flightDate, "B10");
        Thread.sleep(1000);
        kafkaTestProducer.publishDelayEvent(flightNumber, flightDate, 30);
        Thread.sleep(1000);

        // Then: Verify workflow received all signals
        Flight details = workflow.getFlightDetails();
        assertEquals("B10", details.getGate(), "Workflow should have received gate assignment from Kafka");
        assertEquals(30, details.getDelay(), "Workflow should have received delay from Kafka");
    }

    private Flight createTestFlight(String flightNumber, LocalDate flightDate) {
        return new Flight(
                flightNumber,
                flightDate,
                "ORD",
                "DFW",
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(5),
                "A1",
                "N12345"
        );
    }
}
