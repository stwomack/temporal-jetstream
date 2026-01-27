package com.temporal.jetstream.workflow;

import com.temporal.jetstream.model.Flight;
import com.temporal.jetstream.model.FlightState;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.testing.TestWorkflowExtension;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightWorkflowTest {

    @RegisterExtension
    public static final TestWorkflowExtension testWorkflowExtension =
            TestWorkflowExtension.newBuilder()
                    .setWorkflowTypes(FlightWorkflowImpl.class)
                    .setDoNotStart(false)
                    .build();

    @Test
    void testFlightWorkflowExecutionCompletes(
            TestWorkflowEnvironment testEnv, Worker worker, FlightWorkflow workflow) {

        // Create a test flight
        Flight flight = new Flight(
                "AA1234",
                LocalDate.now(),
                "ORD",
                "DFW",
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(5),
                "B12",
                "N12345"
        );

        // Execute the workflow
        Flight result = workflow.executeFlight(flight);

        // Verify the workflow completed successfully
        assertNotNull(result);
        assertEquals("AA1234", result.getFlightNumber());
        assertEquals(FlightState.COMPLETED, result.getCurrentState());
        assertEquals("ORD", result.getDepartureStation());
        assertEquals("DFW", result.getArrivalStation());
    }

    @Test
    void testFlightWorkflowWithWorkflowId() {
        TestWorkflowEnvironment testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker("flight-task-queue");
        worker.registerWorkflowImplementationTypes(FlightWorkflowImpl.class);
        testEnv.start();

        try {
            // Create a test flight
            Flight flight = new Flight(
                    "UA5678",
                    LocalDate.of(2026, 1, 26),
                    "LAX",
                    "SFO",
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusHours(3),
                    "A5",
                    "N67890"
            );

            // Create workflow stub with specific workflow ID
            String workflowId = "flight-UA5678-2026-01-26";
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue("flight-task-queue")
                    .setWorkflowId(workflowId)
                    .build();

            FlightWorkflow workflow = testEnv.getWorkflowClient()
                    .newWorkflowStub(FlightWorkflow.class, options);

            // Execute the workflow
            Flight result = workflow.executeFlight(flight);

            // Verify the workflow completed successfully
            assertNotNull(result);
            assertEquals("UA5678", result.getFlightNumber());
            assertEquals(FlightState.COMPLETED, result.getCurrentState());

            // Verify workflow ID can be retrieved
            WorkflowStub untypedStub = WorkflowStub.fromTyped(workflow);
            assertEquals(workflowId, untypedStub.getExecution().getWorkflowId());
        } finally {
            testEnv.close();
        }
    }

    @Test
    void testAnnounceDelaySignal() {
        TestWorkflowEnvironment testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker("flight-task-queue");
        worker.registerWorkflowImplementationTypes(FlightWorkflowImpl.class);
        testEnv.start();

        try {
            Flight flight = new Flight(
                    "DL9999",
                    LocalDate.now(),
                    "JFK",
                    "LAX",
                    LocalDateTime.now().plusHours(2),
                    LocalDateTime.now().plusHours(6),
                    "C10",
                    "N99999"
            );

            String workflowId = "flight-DL9999-test-delay";
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue("flight-task-queue")
                    .setWorkflowId(workflowId)
                    .build();

            FlightWorkflow workflow = testEnv.getWorkflowClient()
                    .newWorkflowStub(FlightWorkflow.class, options);

            // Start workflow asynchronously
            WorkflowStub untypedStub = WorkflowStub.fromTyped(workflow);
            untypedStub.start(flight);

            // Send delay signal
            workflow.announceDelay(45);

            // Wait for workflow to complete
            Flight result = untypedStub.getResult(Flight.class);

            // Verify delay was recorded
            assertNotNull(result);
            assertEquals(45, result.getDelay());
            assertEquals(FlightState.COMPLETED, result.getCurrentState());
        } finally {
            testEnv.close();
        }
    }

    @Test
    void testChangeGateSignal() {
        TestWorkflowEnvironment testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker("flight-task-queue");
        worker.registerWorkflowImplementationTypes(FlightWorkflowImpl.class);
        testEnv.start();

        try {
            Flight flight = new Flight(
                    "SW1111",
                    LocalDate.now(),
                    "DEN",
                    "PHX",
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusHours(3),
                    "D5",
                    "N11111"
            );

            String workflowId = "flight-SW1111-test-gate";
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue("flight-task-queue")
                    .setWorkflowId(workflowId)
                    .build();

            FlightWorkflow workflow = testEnv.getWorkflowClient()
                    .newWorkflowStub(FlightWorkflow.class, options);

            // Start workflow asynchronously
            WorkflowStub untypedStub = WorkflowStub.fromTyped(workflow);
            untypedStub.start(flight);

            // Send gate change signal
            workflow.changeGate("D15");

            // Wait for workflow to complete
            Flight result = untypedStub.getResult(Flight.class);

            // Verify gate was changed
            assertNotNull(result);
            assertEquals("D15", result.getGate());
            assertEquals(FlightState.COMPLETED, result.getCurrentState());
        } finally {
            testEnv.close();
        }
    }

    @Test
    void testCancelFlightSignal() {
        TestWorkflowEnvironment testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker("flight-task-queue");
        worker.registerWorkflowImplementationTypes(FlightWorkflowImpl.class);
        testEnv.start();

        try {
            Flight flight = new Flight(
                    "BA7777",
                    LocalDate.now(),
                    "BOS",
                    "MIA",
                    LocalDateTime.now().plusHours(3),
                    LocalDateTime.now().plusHours(6),
                    "E20",
                    "N77777"
            );

            String workflowId = "flight-BA7777-test-cancel";
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue("flight-task-queue")
                    .setWorkflowId(workflowId)
                    .build();

            FlightWorkflow workflow = testEnv.getWorkflowClient()
                    .newWorkflowStub(FlightWorkflow.class, options);

            // Start workflow asynchronously
            WorkflowStub untypedStub = WorkflowStub.fromTyped(workflow);
            untypedStub.start(flight);

            // Send cancel signal early in the workflow
            workflow.cancelFlight("Weather conditions");

            // Wait for workflow to complete
            Flight result = untypedStub.getResult(Flight.class);

            // Verify flight was cancelled
            assertNotNull(result);
            assertEquals(FlightState.CANCELLED, result.getCurrentState());
        } finally {
            testEnv.close();
        }
    }

    @Test
    void testMultipleSignals() {
        TestWorkflowEnvironment testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker("flight-task-queue");
        worker.registerWorkflowImplementationTypes(FlightWorkflowImpl.class);
        testEnv.start();

        try {
            Flight flight = new Flight(
                    "AA2222",
                    LocalDate.now(),
                    "ATL",
                    "SEA",
                    LocalDateTime.now().plusHours(2),
                    LocalDateTime.now().plusHours(7),
                    "F8",
                    "N22222"
            );

            String workflowId = "flight-AA2222-test-multi";
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue("flight-task-queue")
                    .setWorkflowId(workflowId)
                    .build();

            FlightWorkflow workflow = testEnv.getWorkflowClient()
                    .newWorkflowStub(FlightWorkflow.class, options);

            // Start workflow asynchronously
            WorkflowStub untypedStub = WorkflowStub.fromTyped(workflow);
            untypedStub.start(flight);

            // Send multiple signals
            workflow.announceDelay(30);
            workflow.changeGate("F12");

            // Wait for workflow to complete
            Flight result = untypedStub.getResult(Flight.class);

            // Verify all signal changes were applied
            assertNotNull(result);
            assertEquals(30, result.getDelay());
            assertEquals("F12", result.getGate());
            assertEquals(FlightState.COMPLETED, result.getCurrentState());
        } finally {
            testEnv.close();
        }
    }

    @Test
    void testGetCurrentStateQuery() {
        TestWorkflowEnvironment testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker("flight-task-queue");
        worker.registerWorkflowImplementationTypes(FlightWorkflowImpl.class);
        testEnv.start();

        try {
            Flight flight = new Flight(
                    "QA3333",
                    LocalDate.now(),
                    "ORD",
                    "LAX",
                    LocalDateTime.now().plusHours(2),
                    LocalDateTime.now().plusHours(5),
                    "G5",
                    "N33333"
            );

            String workflowId = "flight-QA3333-test-query-state";
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue("flight-task-queue")
                    .setWorkflowId(workflowId)
                    .build();

            FlightWorkflow workflow = testEnv.getWorkflowClient()
                    .newWorkflowStub(FlightWorkflow.class, options);

            // Start workflow asynchronously
            WorkflowStub untypedStub = WorkflowStub.fromTyped(workflow);
            untypedStub.start(flight);

            // Query current state multiple times as workflow progresses
            // Note: In test environment, workflow executes very quickly,
            // so we query immediately and verify the workflow started
            FlightState state = workflow.getCurrentState();
            assertNotNull(state);
            assertTrue(state == FlightState.SCHEDULED ||
                      state == FlightState.BOARDING ||
                      state == FlightState.DEPARTED ||
                      state == FlightState.IN_FLIGHT ||
                      state == FlightState.LANDED ||
                      state == FlightState.COMPLETED);

            // Wait for workflow to complete
            Flight result = untypedStub.getResult(Flight.class);

            // Query again after completion
            FlightState finalState = workflow.getCurrentState();
            assertEquals(FlightState.COMPLETED, finalState);
            assertEquals(FlightState.COMPLETED, result.getCurrentState());
        } finally {
            testEnv.close();
        }
    }

    @Test
    void testGetFlightDetailsQuery() {
        TestWorkflowEnvironment testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker("flight-task-queue");
        worker.registerWorkflowImplementationTypes(FlightWorkflowImpl.class);
        testEnv.start();

        try {
            Flight flight = new Flight(
                    "ZZ4444",
                    LocalDate.now(),
                    "SFO",
                    "JFK",
                    LocalDateTime.now().plusHours(3),
                    LocalDateTime.now().plusHours(9),
                    "H10",
                    "N44444"
            );

            String workflowId = "flight-ZZ4444-test-query-details";
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue("flight-task-queue")
                    .setWorkflowId(workflowId)
                    .build();

            FlightWorkflow workflow = testEnv.getWorkflowClient()
                    .newWorkflowStub(FlightWorkflow.class, options);

            // Start workflow asynchronously
            WorkflowStub untypedStub = WorkflowStub.fromTyped(workflow);
            untypedStub.start(flight);

            // Send signals to modify state
            workflow.announceDelay(60);
            workflow.changeGate("H15");

            // Query flight details while workflow is running
            Flight details = workflow.getFlightDetails();
            assertNotNull(details);
            assertEquals("ZZ4444", details.getFlightNumber());
            assertEquals("SFO", details.getDepartureStation());
            assertEquals("JFK", details.getArrivalStation());
            assertEquals(60, details.getDelay());
            assertEquals("H15", details.getGate());

            // Wait for workflow to complete
            Flight result = untypedStub.getResult(Flight.class);

            // Query again after completion
            Flight finalDetails = workflow.getFlightDetails();
            assertEquals(FlightState.COMPLETED, finalDetails.getCurrentState());
            assertEquals(60, finalDetails.getDelay());
            assertEquals("H15", finalDetails.getGate());
        } finally {
            testEnv.close();
        }
    }

    @Test
    void testGetDelayMinutesQuery() {
        TestWorkflowEnvironment testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker("flight-task-queue");
        worker.registerWorkflowImplementationTypes(FlightWorkflowImpl.class);
        testEnv.start();

        try {
            Flight flight = new Flight(
                    "LH5555",
                    LocalDate.now(),
                    "MIA",
                    "DEN",
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusHours(4),
                    "K8",
                    "N55555"
            );

            String workflowId = "flight-LH5555-test-query-delay";
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue("flight-task-queue")
                    .setWorkflowId(workflowId)
                    .build();

            FlightWorkflow workflow = testEnv.getWorkflowClient()
                    .newWorkflowStub(FlightWorkflow.class, options);

            // Start workflow asynchronously
            WorkflowStub untypedStub = WorkflowStub.fromTyped(workflow);
            untypedStub.start(flight);

            // Initially no delay
            int initialDelay = workflow.getDelayMinutes();
            assertEquals(0, initialDelay);

            // Send delay signal
            workflow.announceDelay(90);

            // Query delay after signal
            int updatedDelay = workflow.getDelayMinutes();
            assertEquals(90, updatedDelay);

            // Wait for workflow to complete
            Flight result = untypedStub.getResult(Flight.class);
            assertEquals(90, result.getDelay());

            // Query delay after completion
            int finalDelay = workflow.getDelayMinutes();
            assertEquals(90, finalDelay);
        } finally {
            testEnv.close();
        }
    }

    @Test
    void testQueriesWithSignalsIntegration() {
        TestWorkflowEnvironment testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker("flight-task-queue");
        worker.registerWorkflowImplementationTypes(FlightWorkflowImpl.class);
        testEnv.start();

        try {
            Flight flight = new Flight(
                    "EK6666",
                    LocalDate.now(),
                    "PHX",
                    "BOS",
                    LocalDateTime.now().plusHours(2),
                    LocalDateTime.now().plusHours(7),
                    "M3",
                    "N66666"
            );

            String workflowId = "flight-EK6666-test-query-signal-integration";
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setTaskQueue("flight-task-queue")
                    .setWorkflowId(workflowId)
                    .build();

            FlightWorkflow workflow = testEnv.getWorkflowClient()
                    .newWorkflowStub(FlightWorkflow.class, options);

            // Start workflow asynchronously
            WorkflowStub untypedStub = WorkflowStub.fromTyped(workflow);
            untypedStub.start(flight);

            // Verify initial state via query
            FlightState initialState = workflow.getCurrentState();
            assertNotNull(initialState);
            int initialDelay = workflow.getDelayMinutes();
            assertEquals(0, initialDelay);

            // Send signals
            workflow.announceDelay(45);
            workflow.changeGate("M15");

            // Verify changes via queries
            int delayAfterSignal = workflow.getDelayMinutes();
            assertEquals(45, delayAfterSignal);

            Flight detailsAfterSignal = workflow.getFlightDetails();
            assertEquals("M15", detailsAfterSignal.getGate());
            assertEquals(45, detailsAfterSignal.getDelay());

            // Wait for workflow to complete
            Flight result = untypedStub.getResult(Flight.class);

            // Verify final state via queries
            FlightState finalState = workflow.getCurrentState();
            assertEquals(FlightState.COMPLETED, finalState);
            assertEquals(45, result.getDelay());
            assertEquals("M15", result.getGate());
        } finally {
            testEnv.close();
        }
    }
}
