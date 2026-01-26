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
}
