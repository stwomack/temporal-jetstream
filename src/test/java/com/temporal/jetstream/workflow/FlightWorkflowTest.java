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
}
