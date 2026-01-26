package com.temporal.jetstream.service;

import com.temporal.jetstream.dto.WorkflowHistoryEvent;
import com.temporal.jetstream.model.Flight;
import com.temporal.jetstream.workflow.FlightWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HistoryService workflow history fetching and formatting.
 */
class HistoryServiceTest {

    @RegisterExtension
    public static final TestWorkflowExtension testWorkflowExtension = TestWorkflowExtension.newBuilder()
            .setWorkflowTypes(com.temporal.jetstream.workflow.FlightWorkflowImpl.class)
            .setDoNotStart(false)
            .build();

    @Test
    void testGetWorkflowHistory_ReturnsEvents(
            TestWorkflowEnvironment testEnv,
            Worker worker,
            WorkflowClient workflowClient) {

        // Create HistoryService with test environment
        HistoryService historyService = new HistoryService(
                workflowClient,
                testEnv.getWorkflowServiceStubs()
        );

        // Create and execute a workflow
        Flight flight = new Flight(
                "AA1234",
                LocalDate.now(),
                "ORD",
                "DFW",
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(5),
                "B12",
                "N123AA"
        );

        String workflowId = String.format("flight-%s-%s", flight.getFlightNumber(), flight.getFlightDate());

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(worker.getTaskQueue())
                .build();

        FlightWorkflow workflow = workflowClient.newWorkflowStub(FlightWorkflow.class, options);

        // Execute workflow synchronously
        Flight result = workflow.executeFlight(flight);

        // Verify workflow completed
        assertNotNull(result);

        // Fetch workflow history
        List<WorkflowHistoryEvent> history = historyService.getWorkflowHistory(workflowId);

        // Assertions
        assertNotNull(history);
        assertFalse(history.isEmpty(), "History should contain events");

        // Verify we have the key lifecycle events
        boolean hasWorkflowStarted = history.stream()
                .anyMatch(e -> e.getEventType().contains("WORKFLOW_EXECUTION_STARTED"));
        boolean hasWorkflowCompleted = history.stream()
                .anyMatch(e -> e.getEventType().contains("WORKFLOW_EXECUTION_COMPLETED"));
        boolean hasTimerEvents = history.stream()
                .anyMatch(e -> e.getEventType().contains("TIMER"));

        assertTrue(hasWorkflowStarted, "History should contain WORKFLOW_EXECUTION_STARTED event");
        assertTrue(hasWorkflowCompleted, "History should contain WORKFLOW_EXECUTION_COMPLETED event");
        assertTrue(hasTimerEvents, "History should contain TIMER events (for state transitions)");

        // Verify event structure
        WorkflowHistoryEvent firstEvent = history.get(0);
        assertNotNull(firstEvent.getEventId());
        assertNotNull(firstEvent.getEventType());
        assertNotNull(firstEvent.getTimestamp());
        assertNotNull(firstEvent.getDescription());
        assertNotNull(firstEvent.getCategory());

        // Log history for debugging
        System.out.println("Workflow history events: " + history.size());
        for (WorkflowHistoryEvent event : history) {
            System.out.printf("  [%d] %s - %s (%s)%n",
                    event.getEventId(),
                    event.getTimestamp(),
                    event.getDescription(),
                    event.getCategory());
        }
    }

    @Test
    void testGetWorkflowHistory_WithSignals(
            TestWorkflowEnvironment testEnv,
            Worker worker,
            WorkflowClient workflowClient) {

        // Create HistoryService with test environment
        HistoryService historyService = new HistoryService(
                workflowClient,
                testEnv.getWorkflowServiceStubs()
        );

        // Create and start workflow asynchronously
        Flight flight = new Flight(
                "AA5678",
                LocalDate.now(),
                "LAX",
                "SFO",
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(3),
                "A5",
                "N456AA"
        );

        String workflowId = String.format("flight-%s-%s", flight.getFlightNumber(), flight.getFlightDate());

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(worker.getTaskQueue())
                .build();

        FlightWorkflow workflow = workflowClient.newWorkflowStub(FlightWorkflow.class, options);

        // Start workflow asynchronously
        WorkflowClient.start(workflow::executeFlight, flight);

        // Send signals
        workflow.announceDelay(45);
        workflow.changeGate("C25");

        // Wait for workflow to complete
        Flight result = workflow.executeFlight(flight);
        assertNotNull(result);

        // Fetch workflow history
        List<WorkflowHistoryEvent> history = historyService.getWorkflowHistory(workflowId);

        // Verify signal events are in history
        long signalEvents = history.stream()
                .filter(e -> e.getCategory().equals("signal"))
                .count();

        assertTrue(signalEvents >= 2, "History should contain at least 2 signal events (delay + gate change)");

        // Verify signal descriptions
        boolean hasDelaySignal = history.stream()
                .anyMatch(e -> e.getDescription().contains("announceDelay"));
        boolean hasGateSignal = history.stream()
                .anyMatch(e -> e.getDescription().contains("changeGate"));

        assertTrue(hasDelaySignal, "History should contain announceDelay signal");
        assertTrue(hasGateSignal, "History should contain changeGate signal");
    }

    @Test
    void testGetWorkflowHistory_EventCategories(
            TestWorkflowEnvironment testEnv,
            Worker worker,
            WorkflowClient workflowClient) {

        // Create HistoryService with test environment
        HistoryService historyService = new HistoryService(
                workflowClient,
                testEnv.getWorkflowServiceStubs()
        );

        // Create and execute a workflow
        Flight flight = new Flight(
                "AA9999",
                LocalDate.now(),
                "ORD",
                "LAX",
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(6),
                "D10",
                "N789AA"
        );

        String workflowId = String.format("flight-%s-%s", flight.getFlightNumber(), flight.getFlightDate());

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(worker.getTaskQueue())
                .build();

        FlightWorkflow workflow = workflowClient.newWorkflowStub(FlightWorkflow.class, options);
        Flight result = workflow.executeFlight(flight);

        assertNotNull(result);

        // Fetch workflow history
        List<WorkflowHistoryEvent> history = historyService.getWorkflowHistory(workflowId);

        // Verify different event categories exist
        boolean hasLifecycleEvents = history.stream()
                .anyMatch(e -> e.getCategory().equals("lifecycle"));
        boolean hasTaskEvents = history.stream()
                .anyMatch(e -> e.getCategory().equals("task"));
        boolean hasTimerEvents = history.stream()
                .anyMatch(e -> e.getCategory().equals("timer"));

        assertTrue(hasLifecycleEvents, "History should contain lifecycle events");
        assertTrue(hasTaskEvents, "History should contain task events");
        assertTrue(hasTimerEvents, "History should contain timer events");
    }
}
